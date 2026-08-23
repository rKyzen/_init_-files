package com.init.files.domain.model

/**
 * Represents an encrypted item stored securely inside the Private Vault.
 */
data class VaultItem(
    val id: Long = 0L,
    val originalPath: String,
    val vaultPath: String,
    val name: String,
    val sizeBytes: Long = 0L,
    val encryptedAt: Long = 0L,
    val mimeType: String? = null,
    val isDirectory: Boolean = false
) {
    val formattedSize: String
        get() = formatByteSize(sizeBytes)

    val formattedDate: String
        get() = formatDate(encryptedAt)

    val category: FileCategory
        get() = categorizeFile(name, isDirectory)
}

/**
 * State machine for Vault UI lifecycle.
 */
sealed interface VaultState {
    data object Uninitialized : VaultState
    data object Locked : VaultState
    data class Unlocked(
        val items: List<VaultItem> = emptyList(),
        val selectedIds: Set<Long> = emptySet(),
        val isSelectionMode: Boolean = false,
        val totalEncryptedBytes: Long = 0L
    ) : VaultState
    data class Processing(val message: String, val progress: Float = 0f) : VaultState
}

/**
 * Persistent configuration for Private Vault security.
 */
data class VaultConfig(
    val hasPin: Boolean = false,
    val hint: String? = null,
    val biometricsAvailable: Boolean = false,
    val biometricsEnabled: Boolean = true
)

