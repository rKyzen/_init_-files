package com.init.files

import com.init.files.domain.model.FileCategory
import com.init.files.domain.model.VaultItem
import com.init.files.domain.model.formatByteSize
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class VaultModelsAndCryptoTest {

    @Test
    fun testVaultItemFormatting() {
        val item = VaultItem(
            id = 1L,
            originalPath = "/storage/emulated/0/DCIM/passwords.txt",
            vaultPath = "/data/user/0/com.init.files/files/vault_store/uuid.vault",
            name = "passwords.txt",
            sizeBytes = 1024 * 1024 * 5, // 5MB
            encryptedAt = 1700000000000L,
            mimeType = "text/plain",
            isDirectory = false
        )

        assertEquals("5.0 MB", item.formattedSize)
        assertEquals(FileCategory.DOCUMENTS, item.category)
        assertFalse(item.isDirectory)
    }

    @Test
    fun testPbkdf2AndAes256EncryptionDecryptionCycle() {
        val pin = "1337"
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        // Key derivation
        val spec = PBEKeySpec(pin.toCharArray(), salt, 10_000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        // Encryption
        val originalPlaintext = "CONFIDENTIAL_PAYLOAD_INIT_FILES_2026".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val ivSpec = IvParameterSpec(iv)

        val cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val ciphertext = cipherEncrypt.doFinal(originalPlaintext)

        assertFalse(originalPlaintext.contentEquals(ciphertext))

        // Decryption with same derived key
        val cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipherDecrypt.doFinal(ciphertext)

        assertArrayEquals(originalPlaintext, decryptedBytes)
        assertEquals("CONFIDENTIAL_PAYLOAD_INIT_FILES_2026", String(decryptedBytes, Charsets.UTF_8))
    }

    @Test
    fun testIncorrectPinFailsDecryption() {
        val pin = "1234"
        val wrongPin = "9999"
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

        val key1 = SecretKeySpec(factory.generateSecret(PBEKeySpec(pin.toCharArray(), salt, 10_000, 256)).encoded, "AES")
        val key2 = SecretKeySpec(factory.generateSecret(PBEKeySpec(wrongPin.toCharArray(), salt, 10_000, 256)).encoded, "AES")

        val originalPlaintext = "SECRET_DOCUMENT_BODY".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }

        val cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, key1, IvParameterSpec(iv))
        val ciphertext = cipherEncrypt.doFinal(originalPlaintext)

        val cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherDecrypt.init(Cipher.DECRYPT_MODE, key2, IvParameterSpec(iv))

        var failed = false
        try {
            cipherDecrypt.doFinal(ciphertext)
        } catch (_: Exception) {
            failed = true
        }
        assertTrue("Decryption with wrong key must fail", failed)
    }
}
