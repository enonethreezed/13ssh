package io.enonethreezed.sshclient.model

enum class AppSection {
    WORKSPACE,
    KEYS,
    MACHINES,
    GROUPS,
}

enum class SshAlgorithm {
    ED25519,
    RSA,
    ECDSA,
}

enum class KeySource {
    GENERATED,
    IMPORTED,
}

enum class AuthMethod {
    KEY,
    PASSWORD,
}

data class SshKeySpec(
    val id: String,
    val name: String,
    val algorithm: SshAlgorithm,
    val sizeLabel: String,
    val source: KeySource,
    val fingerprint: String,
    val publicKey: String,
    val privateKey: String,
)

data class MachineProfile(
    val id: String,
    val label: String,
    val host: String,
    val port: Int,
    val username: String,
    val authMethod: AuthMethod,
    val keyId: String?,
    val colorSeed: Long,
)

data class StartupWorkspace(
    val id: String,
    val name: String,
    val profileIds: List<String>,
    val launchOnStartup: Boolean,
)

enum class SessionStatus {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
}

data class SessionTab(
    val id: String,
    val profileId: String,
    val title: String,
    val status: SessionStatus,
    val terminalPreview: List<String>,
)

data class AppData(
    val keys: List<SshKeySpec>,
    val profiles: List<MachineProfile>,
    val workspaces: List<StartupWorkspace>,
    val activeWorkspaceId: String,
    val selectedTabId: String,
)

data class WorkspaceUiState(
    val section: AppSection,
    val data: AppData,
    val tabs: List<SessionTab>,
    val keyOperationInProgress: Boolean = false,
    val toastMessage: String? = null,
) {
    val keys: List<SshKeySpec>
        get() = data.keys

    val profiles: List<MachineProfile>
        get() = data.profiles

    val workspaces: List<StartupWorkspace>
        get() = data.workspaces

    val activeWorkspace: StartupWorkspace?
        get() = workspaces.firstOrNull { it.id == data.activeWorkspaceId }

    val selectedTabId: String
        get() = data.selectedTabId

    val selectedTab: SessionTab?
        get() = tabs.firstOrNull { it.id == selectedTabId }

    fun profileFor(tab: SessionTab?): MachineProfile? {
        if (tab == null) return null
        return profiles.firstOrNull { it.id == tab.profileId }
    }

    fun keyFor(profile: MachineProfile?): SshKeySpec? {
        if (profile == null) return null
        val keyId = profile.keyId ?: return null
        return keys.firstOrNull { it.id == keyId }
    }
}
