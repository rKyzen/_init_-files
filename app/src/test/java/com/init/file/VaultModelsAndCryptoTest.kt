package com.init.file

import com.init.file.domain.model.FileCategory
import com.init.file.domain.model.VaultItem
import com.init.file.domain.model.formatByteSize
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class VaultModelsAndCryptoTest {

    @Test
    fun testVaultItemFormatting() {
        val item = VaultItem(
            id = 1L,
            originalPath = "/storage/emulated/0/DCIM/passwords.txt",
            vaultPath = "/data/user/0/com.init.file/files/vault_store/uuid.vault",
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
    fun testAes256BiometricEncryptionDecryptionCycle() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey: SecretKey = keyGen.generateKey()

        val originalPlaintext = "CONFIDENTIAL_PAYLOAD_INIT_BIOMETRIC_VAULT_2026".toByteArray(Charsets.UTF_8)
        val iv = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val ivSpec = IvParameterSpec(iv)

        val cipherEncrypt = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val ciphertext = cipherEncrypt.doFinal(originalPlaintext)

        assertFalse(originalPlaintext.contentEquals(ciphertext))

        val cipherDecrypt = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        val decryptedBytes = cipherDecrypt.doFinal(ciphertext)

        assertArrayEquals(originalPlaintext, decryptedBytes)
        assertEquals("CONFIDENTIAL_PAYLOAD_INIT_BIOMETRIC_VAULT_2026", String(decryptedBytes, Charsets.UTF_8))
    }

    @Test
    fun testWrongKeyFailsDecryption() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key1 = keyGen.generateKey()
        val key2 = keyGen.generateKey()

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

