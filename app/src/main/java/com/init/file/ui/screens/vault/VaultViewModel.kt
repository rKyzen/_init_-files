package com.init.file.ui.screens.vault

import android.content.Context
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.file.data.vault.VaultManager
import com.init.file.domain.model.FileItem
import com.init.file.domain.model.VaultConfig
import com.init.file.domain.model.VaultItem
import com.init.file.domain.model.VaultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class VaultViewModel(
    private val vaultManager: VaultManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<VaultState>(VaultState.Locked)
    val uiState: StateFlow<VaultState> = _uiState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _vaultConfig = MutableStateFlow(VaultConfig())
    val vaultConfig: StateFlow<VaultConfig> = _vaultConfig.asStateFlow()

    init {
        checkVaultStatus()
    }

    fun checkVaultStatus() {
        viewModelScope.launch {
            val isConfigured = vaultManager.isVaultConfigured()
            val config = vaultManager.getVaultConfig()
            _vaultConfig.value = config

            if (!isConfigured) {
                _uiState.value = VaultState.Uninitialized
            } else if (vaultManager.isUnlocked()) {
                loadVaultItems()
            } else {
                _uiState.value = VaultState.Locked
            }
        }
    }

    fun authenticateWithBiometrics(activity: FragmentActivity, onSucceeded: (() -> Unit)? = null) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModelScope.launch {
                    val success = if (_uiState.value is VaultState.Uninitialized) {
                        vaultManager.initializeBiometricVault()
                    } else {
                        vaultManager.unlockWithBiometrics()
                    }

                    if (success) {
                        _errorMessage.value = null
                        _vaultConfig.value = vaultManager.getVaultConfig()
                        loadVaultItems()
                        onSucceeded?.invoke()
                    } else {
                        _errorMessage.value = "BIOMETRIC AUTHENTICATION FAILED"
                    }
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    _errorMessage.value = errString.toString().uppercase()
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (_uiState.value is VaultState.Uninitialized) "ACTIVATE SAFE VAULT" else "UNLOCK PRIVATE VAULT")
            .setSubtitle("Touch fingerprint sensor or face recognition")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    fun lockVault() {
        vaultManager.lock()
        _errorMessage.value = null
        _uiState.value = VaultState.Locked
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun loadVaultItems() {
        viewModelScope.launch {
            val items = vaultManager.getAllVaultItems()
            val totalBytes = items.sumOf { it.sizeBytes }
            _uiState.value = VaultState.Unlocked(
                items = items,
                selectedIds = emptySet(),
                isSelectionMode = false,
                totalEncryptedBytes = totalBytes
            )
        }
    }

    fun importUris(context: Context, uris: List<Uri>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (!vaultManager.isUnlocked()) {
                val unlocked = vaultManager.unlockWithBiometrics()
                if (!unlocked) {
                    _errorMessage.value = "VAULT MUST BE UNLOCKED FIRST"
                    return@launch
                }
            }

            _uiState.value = VaultState.Processing("ENCRYPTING & IMPORTING FILES...")
            var successCount = 0
            var lastError: String? = null

            for ((index, uri) in uris.withIndex()) {
                val progress = (index + 1).toFloat() / uris.size.toFloat()
                _uiState.value = VaultState.Processing("ENCRYPTING FILE ${index + 1}/${uris.size}...", progress)
                val result = vaultManager.encryptAndVaultUri(context.contentResolver, uri)
                if (result.isSuccess) {
                    successCount++
                } else {
                    lastError = result.exceptionOrNull()?.localizedMessage ?: "Unknown encryption error"
                }
            }

            if (successCount == 0 && lastError != null) {
                _errorMessage.value = "IMPORT FAILED: $lastError"
            } else if (successCount < uris.size && lastError != null) {
                _errorMessage.value = "IMPORTED $successCount/${uris.size} FILES ($lastError)"
            } else {
                _errorMessage.value = null
            }
            loadVaultItems()
            onComplete?.invoke()
        }
    }

    fun importFiles(fileItems: List<FileItem>, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            if (!vaultManager.isUnlocked()) {
                val unlocked = vaultManager.unlockWithBiometrics()
                if (!unlocked) {
                    _errorMessage.value = "VAULT MUST BE UNLOCKED FIRST"
                    return@launch
                }
            }

            _uiState.value = VaultState.Processing("ENCRYPTING & IMPORTING FILES...")
            var successCount = 0
            var lastError: String? = null

            for ((index, item) in fileItems.withIndex()) {
                val file = File(item.path)
                if (file.exists() && file.isFile) {
                    val progress = (index + 1).toFloat() / fileItems.size.toFloat()
                    _uiState.value = VaultState.Processing("ENCRYPTING: ${file.name}", progress)
                    val result = vaultManager.encryptAndVaultFile(file)
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        lastError = result.exceptionOrNull()?.localizedMessage ?: "Encryption failed"
                    }
                }
            }

            if (successCount == 0 && lastError != null) {
                _errorMessage.value = "IMPORT FAILED: $lastError"
            } else if (successCount < fileItems.size && lastError != null) {
                _errorMessage.value = "IMPORTED $successCount/${fileItems.size} FILES ($lastError)"
            } else {
                _errorMessage.value = null
            }
            loadVaultItems()
            onComplete?.invoke()
        }
    }

    fun restoreSelected(selectedIds: Set<Long>, targetDir: File? = null) {
        viewModelScope.launch {
            val current = _uiState.value as? VaultState.Unlocked ?: return@launch
            val itemsToRestore = current.items.filter { selectedIds.contains(it.id) }

            _uiState.value = VaultState.Processing("DECRYPTING & RESTORING FILES...")
            for ((index, item) in itemsToRestore.withIndex()) {
                val progress = (index + 1).toFloat() / itemsToRestore.size.toFloat()
                _uiState.value = VaultState.Processing("RESTORING: ${item.name}", progress)
                vaultManager.restoreVaultItem(item, targetDir)
            }
            loadVaultItems()
        }
    }

    fun deleteSelected(selectedIds: Set<Long>) {
        viewModelScope.launch {
            val current = _uiState.value as? VaultState.Unlocked ?: return@launch
            val itemsToDelete = current.items.filter { selectedIds.contains(it.id) }

            _uiState.value = VaultState.Processing("PERMANENTLY ERASING ENCRYPTED FILES...")
            for (item in itemsToDelete) {
                vaultManager.deleteVaultItem(item)
            }
            loadVaultItems()
        }
    }

    fun decryptAndOpenPreview(item: VaultItem, onDecrypted: (File) -> Unit) {
        viewModelScope.launch {
            val result = vaultManager.decryptToTempCache(item)
            result.onSuccess { decryptedFile ->
                onDecrypted(decryptedFile)
            }.onFailure {
                _errorMessage.value = "DECRYPTION FAILED: ${it.message}"
            }
        }
    }

    fun toggleItemSelection(id: Long) {
        val current = _uiState.value as? VaultState.Unlocked ?: return
        val newSelection = if (current.selectedIds.contains(id)) {
            current.selectedIds - id
        } else {
            current.selectedIds + id
        }
        _uiState.value = current.copy(
            selectedIds = newSelection,
            isSelectionMode = newSelection.isNotEmpty()
        )
    }

    fun selectAll() {
        val current = _uiState.value as? VaultState.Unlocked ?: return
        val allIds = current.items.map { it.id }.toSet()
        _uiState.value = current.copy(
            selectedIds = allIds,
            isSelectionMode = allIds.isNotEmpty()
        )
    }

    fun clearSelection() {
        val current = _uiState.value as? VaultState.Unlocked ?: return
        _uiState.value = current.copy(
            selectedIds = emptySet(),
            isSelectionMode = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        vaultManager.clearTempCache()
    }
}
