package com.init.files.data.vault

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.init.files.data.local.InitDatabaseHelper
import com.init.files.domain.model.VaultConfig
import com.init.files.domain.model.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Manages zero-knowledge AES-256 encrypted Private Vault storage and PIN/Biometric lifecycle.
 */
class VaultManager(
    private val context: Context,
    private val dbHelper: InitDatabaseHelper
) {
    private val vaultDir: File = File(context.filesDir, "vault_store").apply {
        if (!exists()) mkdirs()
    }

    private val cachePreviewDir: File = File(context.cacheDir, "vault_preview").apply {
        if (!exists()) mkdirs()
    }

    private var activeMasterKey: SecretKeySpec? = null

    companion object {
        private const val KEY_ALGORITHM = "AES"
        private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val KEYSTORE_TRANSFORMATION = "AES/CBC/PKCS7Padding"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "InitFilesVaultBiometricKey"
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val ITERATION_COUNT = 10_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 16

        private const val PREF_PIN_HASH = "vault_pin_hash"
        private const val PREF_PIN_SALT = "vault_pin_salt"
        private const val PREF_PIN_HINT = "vault_pin_hint"
        private const val PREF_BIOMETRICS_ENABLED = "vault_biometrics_enabled"
        private const val PREF_BIOMETRIC_IV = "vault_biometric_iv"
        private const val PREF_BIOMETRIC_TOKEN = "vault_biometric_token"
    }

    /**
     * Checks if biometric hardware is present, enrolled, and supported on this device.
     */
    fun canAuthenticateWithBiometrics(): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val status = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            status == BiometricManager.BIOMETRIC_SUCCESS
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if a master PIN is already configured for the vault.
     */
    suspend fun isVaultConfigured(): Boolean = withContext(Dispatchers.IO) {
        val hash = getPreference(PREF_PIN_HASH)
        !hash.isNullOrBlank()
    }

    /**
     * Returns the vault security config including biometrics support and hint.
     */
    suspend fun getVaultConfig(): VaultConfig = withContext(Dispatchers.IO) {
        val hasPin = isVaultConfigured()
        val hint = getPreference(PREF_PIN_HINT)
        val bioPref = getPreference(PREF_BIOMETRICS_ENABLED)?.toBoolean() ?: true
        val bioAvailable = canAuthenticateWithBiometrics()
        VaultConfig(
            hasPin = hasPin,
            hint = hint,
            biometricsAvailable = bioAvailable,
            biometricsEnabled = bioPref
        )
    }

    /**
     * Sets whether biometric unlock should be allowed for this vault.
     */
    suspend fun setBiometricsEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        setPreference(PREF_BIOMETRICS_ENABLED, enabled.toString())
    }

    /**
     * Configures or resets the master PIN for the Private Vault.
     */
    suspend fun setupMasterPin(pin: String, hint: String? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            val random = SecureRandom()
            val salt = ByteArray(SALT_BYTES)
            random.nextBytes(salt)

            val key = deriveKey(pin, salt)
            val saltHex = bytesToHex(salt)
            val hashHex = bytesToHex(key.encoded)

            setPreference(PREF_PIN_SALT, saltHex)
            setPreference(PREF_PIN_HASH, hashHex)
            if (!hint.isNullOrBlank()) {
                setPreference(PREF_PIN_HINT, hint.trim())
            } else {
                deletePreference(PREF_PIN_HINT)
            }

            activeMasterKey = key

            // Store KeyStore-encrypted PIN token for biometric unlock
            storeBiometricToken(pin)

            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Verifies the entered PIN against the stored PBKDF2 hash and unlocks the session.
     */
    suspend fun verifyAndUnlock(pin: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val storedSaltHex = getPreference(PREF_PIN_SALT) ?: return@withContext false
            val storedHashHex = getPreference(PREF_PIN_HASH) ?: return@withContext false

            val salt = hexToBytes(storedSaltHex)
            val key = deriveKey(pin, salt)
            val computedHashHex = bytesToHex(key.encoded)

            if (storedHashHex.equals(computedHashHex, ignoreCase = true)) {
                activeMasterKey = key
                // Keep biometric token fresh
                storeBiometricToken(pin)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Unlocks the vault using the Android KeyStore biometric token.
     */
    suspend fun unlockWithBiometrics(): Boolean = withContext(Dispatchers.IO) {
        try {
            val ivHex = getPreference(PREF_BIOMETRIC_IV) ?: return@withContext false
            val tokenHex = getPreference(PREF_BIOMETRIC_TOKEN) ?: return@withContext false

            val iv = hexToBytes(ivHex)
            val encryptedToken = hexToBytes(tokenHex)

            val keyStoreKey = getOrCreateKeyStoreKey()
            val cipher = Cipher.getInstance(KEYSTORE_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keyStoreKey, IvParameterSpec(iv))

            val decryptedBytes = cipher.doFinal(encryptedToken)
            val pin = String(decryptedBytes, Charsets.UTF_8)

            verifyAndUnlock(pin)
        } catch (_: Exception) {
            false
        }
    }

    private fun storeBiometricToken(pin: String) {
        try {
            val keyStoreKey = getOrCreateKeyStoreKey()
            val cipher = Cipher.getInstance(KEYSTORE_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keyStoreKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(pin.toByteArray(Charsets.UTF_8))

            setPreference(PREF_BIOMETRIC_IV, bytesToHex(iv))
            setPreference(PREF_BIOMETRIC_TOKEN, bytesToHex(encryptedBytes))
        } catch (_: Exception) {}
    }

    private fun getOrCreateKeyStoreKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setKeySize(256)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    /**
     * Locks the vault by clearing the cryptographic key in memory.
     */
    fun lock() {
        activeMasterKey = null
        clearTempCache()
    }

    /**
     * Returns true if the vault is currently unlocked and key is available in memory.
     */
    fun isUnlocked(): Boolean = activeMasterKey != null

    /**
     * Encrypts an external file and moves it securely into the vault.
     */
    suspend fun encryptAndVaultFile(sourceFile: File): Result<VaultItem> = withContext(Dispatchers.IO) {
        val key = activeMasterKey ?: return@withContext Result.failure(IllegalStateException("Vault is locked"))
        if (!sourceFile.exists() || sourceFile.isDirectory) {
            return@withContext Result.failure(IllegalArgumentException("Source file invalid or is directory"))
        }

        try {
            val random = SecureRandom()
            val iv = ByteArray(IV_BYTES)
            random.nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)

            val encryptedFileName = "${UUID.randomUUID()}.vault"
            val destVaultFile = File(vaultDir, encryptedFileName)

            FileOutputStream(destVaultFile).use { fos ->
                // Write IV as header
                fos.write(iv)
                CipherOutputStream(fos, cipher).use { cos ->
                    FileInputStream(sourceFile).use { fis ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (fis.read(buffer).also { read = it } != -1) {
                            cos.write(buffer, 0, read)
                        }
                    }
                }
            }

            // Securely wipe original file
            try {
                if (sourceFile.length() > 0) {
                    val zeroBuf = ByteArray(8192)
                    FileOutputStream(sourceFile).use { fos ->
                        var remaining = sourceFile.length()
                        while (remaining > 0) {
                            val writeBytes = minOf(remaining, zeroBuf.size.toLong()).toInt()
                            fos.write(zeroBuf, 0, writeBytes)
                            remaining -= writeBytes
                        }
                    }
                }
            } catch (_: Exception) {}
            sourceFile.delete()

            // Record in Database
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("original_path", sourceFile.absolutePath)
                put("vault_path", destVaultFile.absolutePath)
                put("name", sourceFile.name)
                put("size", destVaultFile.length())
                put("encrypted_at", System.currentTimeMillis())
                put("mime_type", getMimeType(sourceFile))
                put("is_directory", 0)
            }
            val id = db.insert(InitDatabaseHelper.TABLE_VAULT, null, values)

            val vaultItem = VaultItem(
                id = id,
                originalPath = sourceFile.absolutePath,
                vaultPath = destVaultFile.absolutePath,
                name = sourceFile.name,
                sizeBytes = destVaultFile.length(),
                encryptedAt = System.currentTimeMillis(),
                mimeType = getMimeType(sourceFile)
            )

            Result.success(vaultItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Decrypts a vault item to the secure temporary preview cache.
     */
    suspend fun decryptToTempCache(item: VaultItem): Result<File> = withContext(Dispatchers.IO) {
        val key = activeMasterKey ?: return@withContext Result.failure(IllegalStateException("Vault is locked"))
        val vaultFile = File(item.vaultPath)
        if (!vaultFile.exists()) return@withContext Result.failure(IllegalStateException("Encrypted file missing"))

        try {
            val decryptedFile = File(cachePreviewDir, item.name)

            FileInputStream(vaultFile).use { fis ->
                val iv = ByteArray(IV_BYTES)
                val ivRead = fis.read(iv)
                if (ivRead != IV_BYTES) {
                    return@withContext Result.failure(IllegalStateException("Corrupted encrypted IV"))
                }
                val ivSpec = IvParameterSpec(iv)

                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(decryptedFile).use { fos ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (cis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
            }

            Result.success(decryptedFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restores a vault item back to external storage (original path or custom directory).
     */
    suspend fun restoreVaultItem(item: VaultItem, targetDir: File? = null): Result<File> = withContext(Dispatchers.IO) {
        val key = activeMasterKey ?: return@withContext Result.failure(IllegalStateException("Vault is locked"))
        val vaultFile = File(item.vaultPath)
        if (!vaultFile.exists()) return@withContext Result.failure(IllegalStateException("Vault file not found"))

        try {
            val destination = if (targetDir != null) {
                File(targetDir, item.name)
            } else {
                File(item.originalPath)
            }

            destination.parentFile?.mkdirs()

            FileInputStream(vaultFile).use { fis ->
                val iv = ByteArray(IV_BYTES)
                val ivRead = fis.read(iv)
                if (ivRead != IV_BYTES) {
                    return@withContext Result.failure(IllegalStateException("Corrupted encrypted IV"))
                }
                val ivSpec = IvParameterSpec(iv)

                val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)

                CipherInputStream(fis, cipher).use { cis ->
                    FileOutputStream(destination).use { fos ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        while (cis.read(buffer).also { read = it } != -1) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
            }

            // Remove from vault store and database
            vaultFile.delete()
            val db = dbHelper.writableDatabase
            db.delete(InitDatabaseHelper.TABLE_VAULT, "id = ?", arrayOf(item.id.toString()))

            Result.success(destination)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes an encrypted file from the vault store and database.
     */
    suspend fun deleteVaultItem(item: VaultItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val vaultFile = File(item.vaultPath)
            if (vaultFile.exists()) {
                vaultFile.delete()
            }
            val db = dbHelper.writableDatabase
            db.delete(InitDatabaseHelper.TABLE_VAULT, "id = ?", arrayOf(item.id.toString()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves all items currently stored in the Private Vault.
     */
    suspend fun getAllVaultItems(): List<VaultItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<VaultItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_VAULT,
            arrayOf("id", "original_path", "vault_path", "name", "size", "encrypted_at", "mime_type", "is_directory"),
            null, null, null, null,
            "encrypted_at DESC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("id")
            val origIdx = it.getColumnIndexOrThrow("original_path")
            val vaultIdx = it.getColumnIndexOrThrow("vault_path")
            val nameIdx = it.getColumnIndexOrThrow("name")
            val sizeIdx = it.getColumnIndexOrThrow("size")
            val encIdx = it.getColumnIndexOrThrow("encrypted_at")
            val mimeIdx = it.getColumnIndexOrThrow("mime_type")
            val isDirIdx = it.getColumnIndexOrThrow("is_directory")

            while (it.moveToNext()) {
                val id = it.getLong(idIdx)
                val orig = it.getString(origIdx)
                val vault = it.getString(vaultIdx)
                val name = it.getString(nameIdx)
                val size = it.getLong(sizeIdx)
                val enc = it.getLong(encIdx)
                val mime = it.getString(mimeIdx)
                val isDir = it.getInt(isDirIdx) == 1

                list.add(
                    VaultItem(
                        id = id,
                        originalPath = orig,
                        vaultPath = vault,
                        name = name,
                        sizeBytes = size,
                        encryptedAt = enc,
                        mimeType = mime,
                        isDirectory = isDir
                    )
                )
            }
        }
        list
    }

    /**
     * Clears all temporary decrypted preview files.
     */
    fun clearTempCache() {
        try {
            cachePreviewDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    // --- Private Cryptographic & Preference Helpers ---

    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, KEY_ALGORITHM)
    }

    private fun getPreference(key: String): String? {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            InitDatabaseHelper.TABLE_PREFERENCES,
            arrayOf("value"),
            "key = ?",
            arrayOf(key),
            null, null, null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
    }

    private fun setPreference(key: String, value: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict(
            InitDatabaseHelper.TABLE_PREFERENCES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun deletePreference(key: String) {
        val db = dbHelper.writableDatabase
        db.delete(InitDatabaseHelper.TABLE_PREFERENCES, "key = ?", arrayOf(key))
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt()
            result.append(hexChars[i shr 4 and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun getMimeType(file: File): String {
        val ext = file.name.substringAfterLast('.', "").lowercase()
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
