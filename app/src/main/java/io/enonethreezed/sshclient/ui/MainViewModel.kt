package io.enonethreezed.sshclient.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import io.enonethreezed.sshclient.crypto.SshKeyFactory
import io.enonethreezed.sshclient.data.AppRepository
import io.enonethreezed.sshclient.data.SshBootstrapService
import io.enonethreezed.sshclient.model.AppData
import io.enonethreezed.sshclient.model.AppSection
import io.enonethreezed.sshclient.model.AuthMethod
import io.enonethreezed.sshclient.model.MachineProfile
import io.enonethreezed.sshclient.model.SessionStatus
import io.enonethreezed.sshclient.model.SessionTab
import io.enonethreezed.sshclient.model.SshAlgorithm
import io.enonethreezed.sshclient.model.SshKeySpec
import io.enonethreezed.sshclient.model.StartupWorkspace
import io.enonethreezed.sshclient.model.WorkspaceUiState
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(
    private val repository: AppRepository,
    private val bootstrapService: SshBootstrapService,
) : ViewModel() {
    var uiState by mutableStateOf(buildState(repository.load(), AppSection.WORKSPACE))
        private set

    fun navigateTo(section: AppSection) {
        uiState = uiState.copy(section = section)
    }

    fun selectTab(tabId: String) {
        persist(uiState.data.copy(selectedTabId = tabId))
    }

    fun closeTab(tabId: String) {
        val remaining = uiState.tabs.filterNot { it.id == tabId }
        val nextId = when {
            uiState.selectedTabId != tabId -> uiState.selectedTabId
            remaining.isNotEmpty() -> remaining.first().id
            else -> ""
        }
        persist(uiState.data.copy(selectedTabId = nextId))
    }

    fun activateWorkspace(workspaceId: String) {
        val workspace = uiState.workspaces.firstOrNull { it.id == workspaceId } ?: return
        val selectedTabId = workspace.profileIds.firstOrNull()?.let { "tab-$it" }.orEmpty()
        persist(
            uiState.data.copy(
                activeWorkspaceId = workspaceId,
                selectedTabId = selectedTabId,
            ),
            toast = "Opened ${workspace.name}",
        )
    }

    fun createGeneratedKey(name: String, algorithm: SshAlgorithm, sizeLabel: String) {
        if (name.isBlank()) return
        uiState = uiState.copy(keyOperationInProgress = true, toastMessage = null)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.Default) {
                    SshKeyFactory.generate(name.trim(), algorithm, sizeLabel)
                }
            }
            result.onSuccess { newKey ->
                persist(
                    uiState.data.copy(keys = uiState.keys + newKey),
                    toast = "Generated ${newKey.name}",
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    keyOperationInProgress = false,
                    toastMessage = error.message ?: "Key generation failed.",
                )
            }
        }
    }

    fun importKey(
        name: String,
        publicKey: String,
        privateKey: String,
    ) {
        if (name.isBlank() || publicKey.isBlank() || privateKey.isBlank()) {
            uiState = uiState.copy(toastMessage = "Name, public key, and private key are required.")
            return
        }
        uiState = uiState.copy(keyOperationInProgress = true, toastMessage = null)
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                SshKeyFactory.importValidated(
                    name = name.trim(),
                    publicKeyPem = publicKey.trim(),
                    privateKeyPem = privateKey.trim(),
                )
            }
            result.onSuccess { newKey ->
                persist(
                    uiState.data.copy(keys = uiState.keys + newKey),
                    toast = "Imported ${newKey.name}",
                )
            }.onFailure { error ->
                uiState = uiState.copy(
                    keyOperationInProgress = false,
                    toastMessage = error.message ?: "The key pair is invalid.",
                )
            }
        }
    }

    fun saveMachineProfile(
        label: String,
        host: String,
        portText: String,
        username: String,
        authMethod: AuthMethod,
        keyId: String?,
        bootstrapPassword: String,
    ): Boolean {
        val port = portText.toIntOrNull() ?: 22
        if (label.isBlank() || host.isBlank() || username.isBlank()) {
            uiState = uiState.copy(toastMessage = "Label, host, and username are required.")
            return false
        }
        if (authMethod == AuthMethod.KEY && keyId.isNullOrBlank()) {
            uiState = uiState.copy(toastMessage = "Choose a key for key-based authentication.")
            return false
        }
        if (port !in 1..65535) {
            uiState = uiState.copy(toastMessage = "Port must be between 1 and 65535.")
            return false
        }

        if (authMethod == AuthMethod.KEY) {
            if (bootstrapPassword.isBlank()) {
                uiState = uiState.copy(toastMessage = "Password is required to install the public key.")
                return false
            }
            val selectedKey = uiState.keys.firstOrNull { it.id == keyId }
            if (selectedKey == null) {
                uiState = uiState.copy(toastMessage = "Selected key was not found.")
                return false
            }
            uiState = uiState.copy(keyOperationInProgress = true, toastMessage = null)
            viewModelScope.launch {
                val bootstrap = withContext(Dispatchers.IO) {
                    bootstrapService.bootstrapWithPasswordThenKey(
                        host = host.trim(),
                        port = port,
                        username = username.trim(),
                        password = bootstrapPassword,
                        publicKey = selectedKey.publicKey,
                        privateKey = selectedKey.privateKey,
                    )
                }
                bootstrap.onSuccess {
                    persist(
                        uiState.data.copy(
                            profiles = uiState.profiles + MachineProfile(
                                id = "profile-${UUID.randomUUID()}",
                                label = label.trim(),
                                host = host.trim(),
                                port = port,
                                username = username.trim(),
                                authMethod = authMethod,
                                keyId = keyId,
                                colorSeed = stableColor(label.trim()),
                            )
                        ),
                        toast = "Provisioned and saved ${label.trim()} using key authentication.",
                    )
                }.onFailure { error ->
                    uiState = uiState.copy(
                        keyOperationInProgress = false,
                        toastMessage = error.message ?: "Host provisioning failed.",
                    )
                }
            }
            return true
        }

        val profile = MachineProfile(
            id = "profile-${UUID.randomUUID()}",
            label = label.trim(),
            host = host.trim(),
            port = port,
            username = username.trim(),
            authMethod = authMethod,
            keyId = keyId,
            colorSeed = stableColor(label.trim()),
        )
        persist(
            uiState.data.copy(profiles = uiState.profiles + profile),
            toast = "Saved ${profile.label}",
        )
        return true
    }

    fun saveWorkspace(name: String, profileIds: List<String>, launchOnStartup: Boolean) {
        if (name.isBlank() || profileIds.isEmpty()) return
        val workspaces = uiState.workspaces.map {
            if (launchOnStartup) it.copy(launchOnStartup = false) else it
        } + StartupWorkspace(
            id = "workspace-${UUID.randomUUID()}",
            name = name.trim(),
            profileIds = profileIds,
            launchOnStartup = launchOnStartup,
        )
        val activeWorkspaceId = if (launchOnStartup) workspaces.last().id else uiState.data.activeWorkspaceId
        val selectedTabId = if (launchOnStartup) {
            profileIds.firstOrNull()?.let { "tab-$it" }.orEmpty()
        } else {
            uiState.selectedTabId
        }
        persist(
            uiState.data.copy(
                workspaces = workspaces,
                activeWorkspaceId = activeWorkspaceId,
                selectedTabId = selectedTabId,
            ),
            toast = "Saved launch group ${name.trim()}",
        )
    }

    fun dismissToast() {
        uiState = uiState.copy(toastMessage = null)
    }

    private fun persist(data: AppData, toast: String? = null) {
        repository.save(data)
        uiState = buildState(data, uiState.section, toast, keyOperationInProgress = false)
    }

    private fun buildState(
        data: AppData,
        section: AppSection,
        toast: String? = null,
        keyOperationInProgress: Boolean = false,
    ): WorkspaceUiState {
        val activeWorkspace = data.workspaces.firstOrNull { it.id == data.activeWorkspaceId }
            ?: data.workspaces.firstOrNull()
        val tabs = activeWorkspace.orEmptyTabs(data.profiles, data.keys)
        val selected = tabs.firstOrNull { it.id == data.selectedTabId }?.id
            ?: tabs.firstOrNull()?.id.orEmpty()
        val normalized = if (selected == data.selectedTabId) data else data.copy(selectedTabId = selected)
        return WorkspaceUiState(
            section = section,
            data = normalized,
            tabs = tabs,
            keyOperationInProgress = keyOperationInProgress,
            toastMessage = toast,
        )
    }

    companion object {
        fun factory(
            repository: AppRepository,
            bootstrapService: SshBootstrapService,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return MainViewModel(repository, bootstrapService) as T
                }
                error("Unsupported ViewModel class: ${modelClass.name}")
            }
        }
    }
}

private fun StartupWorkspace?.orEmptyTabs(
    profiles: List<MachineProfile>,
    keys: List<SshKeySpec>,
): List<SessionTab> {
    if (this == null) return emptyList()
    return profileIds.mapNotNull { profileId ->
        val profile = profiles.firstOrNull { it.id == profileId } ?: return@mapNotNull null
        val key = profile.keyId?.let { keyId -> keys.firstOrNull { it.id == keyId } }
        SessionTab(
            id = "tab-${profile.id}",
            profileId = profile.id,
            title = profile.label,
            status = when (profile.label.length % 3) {
                0 -> SessionStatus.CONNECTING
                1 -> SessionStatus.CONNECTED
                else -> SessionStatus.DISCONNECTED
            },
            terminalPreview = listOf(
                "ssh ${profile.username}@${profile.host} -p ${profile.port}",
                if (profile.authMethod == AuthMethod.KEY) {
                    "identity ${key?.name ?: "No key assigned"}"
                } else {
                    "authentication password prompt"
                },
                "${profile.username}@${profile.label.lowercase()}:~$ ",
            ),
        )
    }
}

private fun stableColor(value: String): Long {
    val hash = value.fold(0L) { acc, char -> acc * 31 + char.code }
    return 0x202020 + (hash and 0xAFAFAF)
}
