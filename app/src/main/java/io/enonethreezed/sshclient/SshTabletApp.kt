package io.enonethreezed.sshclient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.enonethreezed.sshclient.data.AppRepository
import io.enonethreezed.sshclient.data.SshBootstrapService
import io.enonethreezed.sshclient.ui.MainViewModel
import io.enonethreezed.sshclient.ui.workspace.WorkspaceScreen
import io.enonethreezed.sshclient.ui.theme.ThirteenSshTheme

@Composable
fun SshTabletApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember { AppRepository(context) }
    val bootstrapService = remember { SshBootstrapService(context) }
    val viewModel: MainViewModel = viewModel(factory = MainViewModel.factory(repository, bootstrapService))

    ThirteenSshTheme {
        WorkspaceScreen(
            state = viewModel.uiState,
            onNavigate = viewModel::navigateTo,
            onSelectTab = viewModel::selectTab,
            onCloseTab = viewModel::closeTab,
            onCreateGeneratedKey = viewModel::createGeneratedKey,
            onImportKey = viewModel::importKey,
            onSaveMachineProfile = viewModel::saveMachineProfile,
            onSaveWorkspace = viewModel::saveWorkspace,
            onActivateWorkspace = viewModel::activateWorkspace,
            onDismissToast = viewModel::dismissToast,
        )
    }
}
