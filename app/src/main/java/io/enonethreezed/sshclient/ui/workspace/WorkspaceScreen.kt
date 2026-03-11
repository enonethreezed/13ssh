package io.enonethreezed.sshclient.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.enonethreezed.sshclient.model.AppSection
import io.enonethreezed.sshclient.model.AuthMethod
import io.enonethreezed.sshclient.model.MachineProfile
import io.enonethreezed.sshclient.model.SessionStatus
import io.enonethreezed.sshclient.model.SessionTab
import io.enonethreezed.sshclient.model.SshAlgorithm
import io.enonethreezed.sshclient.model.SshKeySpec
import io.enonethreezed.sshclient.model.StartupWorkspace
import io.enonethreezed.sshclient.model.WorkspaceUiState
import io.enonethreezed.sshclient.ui.theme.AlertAmber
import io.enonethreezed.sshclient.ui.theme.AlertRed
import io.enonethreezed.sshclient.ui.theme.Fog
import io.enonethreezed.sshclient.ui.theme.Mist

@Composable
fun WorkspaceScreen(
    state: WorkspaceUiState,
    onNavigate: (AppSection) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateGeneratedKey: (String, SshAlgorithm, String) -> Unit,
    onImportKey: (String, String, String) -> Unit,
    onSaveMachineProfile: (String, String, String, String, AuthMethod, String?, String) -> Boolean,
    onSaveWorkspace: (String, List<String>, Boolean) -> Unit,
    onActivateWorkspace: (String) -> Unit,
    onDismissToast: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WorkspaceHeader(state = state, onDismissToast = onDismissToast)
            SectionNav(selected = state.section, onNavigate = onNavigate)
            when (state.section) {
                AppSection.WORKSPACE -> WorkspaceDashboard(
                    state = state,
                    onSelectTab = onSelectTab,
                    onCloseTab = onCloseTab,
                    onActivateWorkspace = onActivateWorkspace,
                )

                AppSection.KEYS -> KeysScreen(
                    state = state,
                    onCreateGeneratedKey = onCreateGeneratedKey,
                    onImportKey = onImportKey,
                )

                AppSection.MACHINES -> MachinesScreen(
                    state = state,
                    onSaveMachineProfile = onSaveMachineProfile,
                )

                AppSection.GROUPS -> GroupsScreen(
                    state = state,
                    onSaveWorkspace = onSaveWorkspace,
                    onActivateWorkspace = onActivateWorkspace,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceHeader(state: WorkspaceUiState, onDismissToast: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "13SSH", style = MaterialTheme.typography.labelMedium, color = Fog)
            Text(
                text = state.activeWorkspace?.name ?: "No launch group selected",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Manage keys, machine profiles, and startup groups from the same tablet workspace.",
                style = MaterialTheme.typography.titleMedium,
                color = Mist.copy(alpha = 0.84f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text("${state.keys.size} keys") },
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        labelColor = Mist,
                        leadingIconContentColor = Mist,
                    ),
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${state.profiles.size} machines") },
                    leadingIcon = { Icon(Icons.Rounded.Computer, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        labelColor = Mist,
                        leadingIconContentColor = Mist,
                    ),
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${state.workspaces.size} groups") },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
                        labelColor = Mist,
                        leadingIconContentColor = Mist,
                    ),
                )
            }
            if (state.toastMessage != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.toastMessage,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismissToast) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss message")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectionNav(selected: AppSection, onNavigate: (AppSection) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppSection.entries.forEach { section ->
            FilterChip(
                selected = section == selected,
                onClick = { onNavigate(section) },
                label = { Text(section.name.lowercase().replaceFirstChar(Char::titlecase)) },
            )
        }
    }
}

@Composable
private fun WorkspaceDashboard(
    state: WorkspaceUiState,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onActivateWorkspace: (String) -> Unit,
) {
    if (state.workspaces.isEmpty()) {
        EmptyWorkspaceState(modifier = Modifier.fillMaxSize())
        return
    }

    val selectedTab = state.selectedTab
    val selectedProfile = state.profileFor(selectedTab)
    val selectedKey = state.keyFor(selectedProfile)

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.weight(1.8f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TabStrip(
                tabs = state.tabs,
                selectedTabId = state.selectedTabId,
                profiles = state.profiles.associateBy(MachineProfile::id),
                onSelectTab = onSelectTab,
                onCloseTab = onCloseTab,
            )
            SessionDetails(
                modifier = Modifier.weight(1f),
                tab = selectedTab,
                profile = selectedProfile,
                key = selectedKey,
            )
        }
        WorkspaceSidebar(
            modifier = Modifier.weight(1f),
            state = state,
            onActivateWorkspace = onActivateWorkspace,
        )
    }
}

@Composable
private fun EmptyWorkspaceState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "No launch groups yet", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create a key, then a machine profile, and finally a launch group to open several SSH tabs together.",
                style = MaterialTheme.typography.titleMedium,
                color = Fog,
            )
        }
    }
}

@Composable
private fun TabStrip(
    tabs: List<SessionTab>,
    selectedTabId: String,
    profiles: Map<String, MachineProfile>,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tabs.forEach { tab ->
            val profile = profiles[tab.profileId]
            val isSelected = tab.id == selectedTabId
            Card(
                modifier = Modifier.width(180.dp),
                onClick = { onSelectTab(tab.id) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    }
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(status = tab.status, colorSeed = profile?.colorSeed ?: 0L)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = tab.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = profile?.host.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Fog,
                        )
                    }
                    IconButton(onClick = { onCloseTab(tab.id) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close tab")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(status: SessionStatus, colorSeed: Long) {
    val base = Color((0xFF000000 or colorSeed).toInt())
    val color = when (status) {
        SessionStatus.CONNECTED -> base
        SessionStatus.CONNECTING -> AlertAmber
        SessionStatus.DISCONNECTED -> AlertRed
    }
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun SessionDetails(
    modifier: Modifier = Modifier,
    tab: SessionTab?,
    profile: MachineProfile?,
    key: SshKeySpec?,
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = tab?.title ?: "No active session",
                style = MaterialTheme.typography.headlineMedium,
            )
            if (profile != null && tab != null) {
                Text(
                    text = "${profile.username}@${profile.host}:${profile.port}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Fog,
                )
                Text(
                    text = if (profile.authMethod == AuthMethod.KEY) {
                        "Key: ${key?.name ?: "None"}  ${key?.fingerprint.orEmpty()}"
                    } else {
                        "Auth: Password (not stored)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = Mist.copy(alpha = 0.8f),
                )
                if (profile.authMethod == AuthMethod.PASSWORD) {
                    var sessionPassword by rememberSaveable(profile.id) { mutableStateOf("") }
                    OutlinedTextField(
                        value = sessionPassword,
                        onValueChange = { sessionPassword = it },
                        label = { Text("Session password") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Used only for this running session and never persisted.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Fog,
                    )
                }
                HorizontalDivider(color = Mist.copy(alpha = 0.08f))
                TerminalPreview(lines = tab.terminalPreview)
            }
        }
    }
}

@Composable
private fun TerminalPreview(lines: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0A0D10))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        lines.forEach { line ->
            Text(text = line, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "SSH transport is the next backend layer. Profiles, keys, and launch groups are already wired through the app state.",
            style = MaterialTheme.typography.labelMedium,
            color = Fog,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkspaceSidebar(
    modifier: Modifier = Modifier,
    state: WorkspaceUiState,
    onActivateWorkspace: (String) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            SidebarBlock(title = "Launch Groups") {
                state.workspaces.forEach { workspace ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = workspace.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${workspace.profileIds.size} tabs${if (workspace.launchOnStartup) "  startup" else ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Fog,
                            )
                        }
                        Button(onClick = { onActivateWorkspace(workspace.id) }) {
                            Text("Open")
                        }
                    }
                }
            }
            SidebarBlock(title = "Machine Profiles") {
                state.profiles.forEach { profile ->
                    Text(
                        text = "${profile.label}  ${profile.username}@${profile.host}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            SidebarBlock(title = "SSH Keys") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.keys.forEach { key ->
                        AssistChip(
                            onClick = {},
                            label = { Text("${key.name} ${key.sizeLabel}") },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                labelColor = Mist,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeysScreen(
    state: WorkspaceUiState,
    onCreateGeneratedKey: (String, SshAlgorithm, String) -> Unit,
    onImportKey: (String, String, String) -> Unit,
) {
    var generatedName by rememberSaveable { mutableStateOf("") }
    var generatedAlgorithm by rememberSaveable { mutableStateOf(SshAlgorithm.ED25519) }
    var generatedSize by rememberSaveable { mutableStateOf(sizeOptions(SshAlgorithm.ED25519).first()) }

    var importedName by rememberSaveable { mutableStateOf("") }
    var importedPublic by rememberSaveable { mutableStateOf("") }
    var importedPrivate by rememberSaveable { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CardColumn(modifier = Modifier.weight(1f), title = "Generate key pair") {
            if (state.keyOperationInProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = generatedName,
                onValueChange = { generatedName = it },
                label = { Text("Key name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.keyOperationInProgress,
            )
            AlgorithmChooser(
                selected = generatedAlgorithm,
                onSelect = {
                    generatedAlgorithm = it
                    generatedSize = sizeOptions(it).first()
                },
                enabled = !state.keyOperationInProgress,
            )
            ChoiceRow(
                title = "Bit size",
                selected = generatedSize,
                options = sizeOptions(generatedAlgorithm),
                enabled = !state.keyOperationInProgress,
                onSelect = { generatedSize = it },
            )
            Button(
                onClick = {
                    onCreateGeneratedKey(generatedName, generatedAlgorithm, generatedSize)
                    generatedName = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.keyOperationInProgress,
            ) {
                Text(if (state.keyOperationInProgress) "Working..." else "Generate")
            }
        }

        CardColumn(modifier = Modifier.weight(1f), title = "Import key pair") {
            if (state.keyOperationInProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(
                value = importedName,
                onValueChange = { importedName = it },
                label = { Text("Key name") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.keyOperationInProgress,
            )
            Text(
                text = "Paste the public and private PEM blocks. The app validates that both belong to the same pair and infers the algorithm and bit size automatically.",
                style = MaterialTheme.typography.labelMedium,
                color = Fog,
            )
            OutlinedTextField(
                value = importedPublic,
                onValueChange = { importedPublic = it },
                label = { Text("Public key") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                enabled = !state.keyOperationInProgress,
            )
            OutlinedTextField(
                value = importedPrivate,
                onValueChange = { importedPrivate = it },
                label = { Text("Private key") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                enabled = !state.keyOperationInProgress,
            )
            Button(
                onClick = {
                    onImportKey(importedName, importedPublic, importedPrivate)
                    importedName = ""
                    importedPublic = ""
                    importedPrivate = ""
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.keyOperationInProgress,
            ) {
                Text(if (state.keyOperationInProgress) "Working..." else "Import")
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    CardColumn(modifier = Modifier.fillMaxWidth(), title = "Available keys") {
        if (state.keys.isEmpty()) {
            Text(text = "No keys yet.", style = MaterialTheme.typography.bodyMedium, color = Fog)
        } else {
            state.keys.forEach { key ->
                KeyCard(key = key)
            }
        }
    }
}

@Composable
private fun MachinesScreen(
    state: WorkspaceUiState,
    onSaveMachineProfile: (String, String, String, String, AuthMethod, String?, String) -> Boolean,
) {
    val clipboard = LocalClipboardManager.current
    val oneTimePasswords = remember { mutableStateMapOf<String, String>() }
    var label by rememberSaveable { mutableStateOf("") }
    var host by rememberSaveable { mutableStateOf("") }
    var port by rememberSaveable { mutableStateOf("22") }
    var username by rememberSaveable { mutableStateOf("") }
    var authMethod by rememberSaveable { mutableStateOf(AuthMethod.KEY) }
    var selectedKeyId by rememberSaveable { mutableStateOf(state.keys.firstOrNull()?.id.orEmpty()) }
    var bootstrapPassword by rememberSaveable { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CardColumn(modifier = Modifier.weight(1f), title = "Create machine profile") {
            if (state.keys.isEmpty() && authMethod == AuthMethod.KEY) {
                Text(text = "Generate or import a key pair first.", style = MaterialTheme.typography.bodyMedium, color = Fog)
            }
            OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
            ChoiceRow(
                title = "Authentication",
                selected = authMethod.name,
                options = AuthMethod.entries.map { it.name },
                labels = mapOf(AuthMethod.KEY.name to "SSH Key", AuthMethod.PASSWORD.name to "Password"),
                onSelect = { authMethod = AuthMethod.valueOf(it) },
            )
            if (authMethod == AuthMethod.KEY) {
                ChoiceRow(
                    title = "Assigned key",
                    selected = selectedKeyId,
                    options = state.keys.map { it.id },
                    labels = state.keys.associate { it.id to it.name },
                    onSelect = { selectedKeyId = it },
                )
                OutlinedTextField(
                    value = bootstrapPassword,
                    onValueChange = { bootstrapPassword = it },
                    label = { Text("Bootstrap password (one-time)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Required to connect once, install the public key, disconnect, and reconnect using key authentication.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Fog,
                )
            } else {
                Text(
                    text = "Password is requested at connection time only and is never saved.",
                    style = MaterialTheme.typography.labelMedium,
                    color = Fog,
                )
            }
            Button(
                onClick = {
                    val saved = onSaveMachineProfile(
                        label,
                        host,
                        port,
                        username,
                        authMethod,
                        if (authMethod == AuthMethod.KEY) selectedKeyId else null,
                        bootstrapPassword,
                    )
                    if (saved) {
                        label = ""
                        host = ""
                        port = "22"
                        username = ""
                        bootstrapPassword = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authMethod == AuthMethod.PASSWORD || state.keys.isNotEmpty(),
            ) {
                Text(if (authMethod == AuthMethod.KEY) "Provision host and save" else "Save machine")
            }
        }

        CardColumn(modifier = Modifier.weight(1f), title = "Saved machines") {
            if (state.profiles.isEmpty()) {
                Text(text = "No machine profiles yet.", style = MaterialTheme.typography.bodyMedium, color = Fog)
            } else {
                state.profiles.forEach { profile ->
                    val key = state.keys.firstOrNull { it.id == profile.keyId }
                    Text(text = profile.label, style = MaterialTheme.typography.titleMedium)
                    Text(text = "${profile.username}@${profile.host}:${profile.port}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = if (profile.authMethod == AuthMethod.KEY) {
                            "Auth: key ${key?.name ?: "missing"}"
                        } else {
                            "Auth: password (not saved)"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = Fog,
                    )
                    if (profile.authMethod == AuthMethod.KEY && key != null) {
                        val profilePassword = oneTimePasswords[profile.id].orEmpty()
                        OutlinedTextField(
                            value = profilePassword,
                            onValueChange = { oneTimePasswords[profile.id] = it },
                            label = { Text("One-time server password") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = {
                                val command = sshCopyIdCommand(profile, key, profilePassword)
                                clipboard.setText(AnnotatedString(command))
                            },
                        ) {
                            Text("Copy ssh-copy-id command")
                        }
                        Text(
                            text = "Password is only used in the copied command, never stored in app data.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Fog,
                        )
                    }
                    HorizontalDivider(color = Mist.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun GroupsScreen(
    state: WorkspaceUiState,
    onSaveWorkspace: (String, List<String>, Boolean) -> Unit,
    onActivateWorkspace: (String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var startup by rememberSaveable { mutableStateOf(false) }
    var selectedProfileIds by remember { mutableStateOf(state.activeWorkspace?.profileIds ?: emptyList()) }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CardColumn(modifier = Modifier.weight(1f), title = "Create launch group") {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group name") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Open on startup", modifier = Modifier.weight(1f))
                Switch(checked = startup, onCheckedChange = { startup = it })
            }
            ProfileSelector(
                profiles = state.profiles,
                selectedIds = selectedProfileIds,
                onToggle = { profileId ->
                    selectedProfileIds = if (selectedProfileIds.contains(profileId)) {
                        selectedProfileIds - profileId
                    } else {
                        selectedProfileIds + profileId
                    }
                },
            )
            Button(
                onClick = {
                    onSaveWorkspace(name, selectedProfileIds, startup)
                    name = ""
                    startup = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedProfileIds.isNotEmpty(),
            ) {
                Text("Save group")
            }
        }

        CardColumn(modifier = Modifier.weight(1f), title = "Saved groups") {
            if (state.workspaces.isEmpty()) {
                Text(text = "No launch groups yet.", style = MaterialTheme.typography.bodyMedium, color = Fog)
            } else {
                state.workspaces.forEach { workspace ->
                    val labels = workspace.profileIds.mapNotNull { id -> state.profiles.firstOrNull { it.id == id }?.label }
                    Text(text = workspace.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = labels.joinToString(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (workspace.launchOnStartup) "Startup default" else "Manual launch",
                        style = MaterialTheme.typography.labelMedium,
                        color = Fog,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { onActivateWorkspace(workspace.id) }) {
                        Text("Open group")
                    }
                    HorizontalDivider(color = Mist.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun KeyCard(key: SshKeySpec) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = key.name, style = MaterialTheme.typography.titleMedium)
            Text(text = "${key.algorithm.name}  ${key.sizeLabel}  ${key.source.name.lowercase()}", style = MaterialTheme.typography.labelMedium, color = Fog)
            Text(text = key.fingerprint, style = MaterialTheme.typography.bodyMedium)
            Text(text = key.publicKey.lineSequence().firstOrNull().orEmpty(), style = MaterialTheme.typography.labelMedium, color = Mist.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun CardColumn(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .requiredHeightIn(min = 180.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(text = title, style = MaterialTheme.typography.headlineMedium)
                content()
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlgorithmChooser(selected: SshAlgorithm, onSelect: (SshAlgorithm) -> Unit, enabled: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SshAlgorithm.entries.forEach { algorithm ->
            FilterChip(selected = algorithm == selected, onClick = { onSelect(algorithm) }, label = { Text(algorithm.name) }, enabled = enabled)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceRow(
    title: String,
    selected: String,
    options: List<String>,
    labels: Map<String, String> = emptyMap(),
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(labels[option] ?: option) },
                    enabled = enabled,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSelector(
    profiles: List<MachineProfile>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Profiles in this group", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            profiles.forEach { profile ->
                FilterChip(
                    selected = selectedIds.contains(profile.id),
                    onClick = { onToggle(profile.id) },
                    label = { Text(profile.label) },
                )
            }
        }
    }
}

@Composable
private fun SidebarBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

private fun sizeOptions(algorithm: SshAlgorithm): List<String> = when (algorithm) {
    SshAlgorithm.ED25519 -> listOf("255-bit (Ed25519)")
    SshAlgorithm.RSA -> listOf("2048-bit", "3072-bit", "4096-bit")
    SshAlgorithm.ECDSA -> listOf("256-bit (P-256)", "384-bit (P-384)", "521-bit (P-521)")
}

private fun sshCopyIdCommand(profile: MachineProfile, key: SshKeySpec, password: String): String {
    val escapedPublic = key.publicKey
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("$", "\\$")
    val remote = "mkdir -p ~/.ssh && chmod 700 ~/.ssh && echo \"$escapedPublic\" >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
    return if (password.isNotBlank()) {
        val escapedPassword = password
            .replace("'", "'\\''")
        "sshpass -p '$escapedPassword' ssh ${profile.username}@${profile.host} -p ${profile.port} \"$remote\""
    } else {
        "ssh ${profile.username}@${profile.host} -p ${profile.port} \"$remote\""
    }
}
