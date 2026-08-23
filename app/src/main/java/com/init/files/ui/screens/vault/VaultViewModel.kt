package com.init.files.ui.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.init.files.data.vault.VaultManager
import com.init.files.domain.model.FileItem
import com.init.files.domain.model.VaultConfig
import com.init.files.domain.model.VaultItem
import com.init.files.domain.model.VaultState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class VaultSetupStep {
    ENTER_NEW_PIN,
    CONFIRM_NEW_PIN,
    ENTER_HINT
}

class VaultViewModel(
    private val vaultManager: VaultManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<VaultState>(VaultState.Locked)
    val uiState: StateFlow<VaultState> = _uiState.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _setupStep = MutableStateFlow(VaultSetupStep.ENTER_NEW_PIN)
    val setupStep: StateFlow<VaultSetupStep> = _setupStep.asStateFlow()

    private val _hintInput = MutableStateFlow("")
    val hintInput: StateFlow<String> = _hintInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _vaultConfig = MutableStateFlow(VaultConfig())
    val vaultConfig: StateFlow<VaultConfig> = _vaultConfig.asStateFlow()

    private var tempNewPin: String = ""

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
                _setupStep.value = VaultSetupStep.ENTER_NEW_PIN
            } else if (vaultManager.isUnlocked()) {
                loadVaultItems()
            } else {
                _uiState.value = VaultState.Locked
            }
        }
    }

    fun onDigitPress(digit: String) {
        if (_pinInput.value.length < 6) {
            _pinInput.value += digit
            _errorMessage.value = null

            // Auto-submit on 4 or 6 digits if desired or upon manual submit
            if (_pinInput.value.length == 4 && _uiState.value is VaultState.Locked) {
                submitPin()
            }
        }
    }

    fun onBackspacePress() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
            _errorMessage.value = null
        }
    }

    fun onClearPress() {
        _pinInput.value = ""
        _errorMessage.value = null
    }

    fun onHintChange(hint: String) {
        _hintInput.value = hint
    }

    fun submitPin() {
        val pin = _pinInput.value.trim()
        if (pin.length < 4) {
            _errorMessage.value = "PIN MUST BE AT LEAST 4 DIGITS"
            return
        }

        viewModelScope.launch {
            when (_uiState.value) {
                is VaultState.Uninitialized -> {
                    when (_setupStep.value) {
                        VaultSetupStep.ENTER_NEW_PIN -> {
                            tempNewPin = pin
                            _pinInput.value = ""
                            _setupStep.value = VaultSetupStep.CONFIRM_NEW_PIN
                        }
                        VaultSetupStep.CONFIRM_NEW_PIN -> {
                            if (pin == tempNewPin) {
                                _pinInput.value = ""
                                _setupStep.value = VaultSetupStep.ENTER_HINT
                            } else {
                                _errorMessage.value = "PINS DO NOT MATCH. RETRY."
                                _pinInput.value = ""
                                _setupStep.value = VaultSetupStep.ENTER_NEW_PIN
                            }
                        }
                        VaultSetupStep.ENTER_HINT -> {
                            finalizeVaultSetup()
                        }
                    }
                }
                is VaultState.Locked -> {
                    val success = vaultManager.verifyAndUnlock(pin)
                    if (success) {
                        _pinInput.value = ""
                        _errorMessage.value = null
                        loadVaultItems()
                    } else {
                        _errorMessage.value = "AUTHENTICATION FAILED: INVALID PIN"
                        _pinInput.value = ""
                    }
                }
                else -> {}
            }
        }
    }

    fun finalizeVaultSetup() {
        viewModelScope.launch {
            val hint = _hintInput.value.trim().takeIf { it.isNotBlank() }
            val success = vaultManager.setupMasterPin(tempNewPin, hint)
            if (success) {
                _pinInput.value = ""
                _hintInput.value = ""
                _vaultConfig.value = vaultManager.getVaultConfig()
                loadVaultItems()
            } else {
                _errorMessage.value = "FAILED TO INITIALIZE VAULT ENCRYPTION"
            }
        }
    }

    fun lockVault() {
        vaultManager.lock()
        _pinInput.value = ""
        _errorMessage.value = null
        _uiState.value = VaultState.Locked
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

    fun importFiles(fileItems: List<FileItem>) {
        viewModelScope.launch {
            _uiState.value = VaultState.Processing("ENCRYPTING & IMPORTING FILES...")
            var successCount = 0
            for ((index, item) in fileItems.withIndex()) {
                val file = File(item.path)
                if (file.exists() && file.isFile) {
                    val progress = (index + 1).toFloat() / fileItems.size.toFloat()
                    _uiState.value = VaultState.Processing("ENCRYPTING: ${file.name}", progress)
                    val result = vaultManager.encryptAndVaultFile(file)
                    if (result.isSuccess) successCount++
                }
            }
            loadVaultItems()
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
