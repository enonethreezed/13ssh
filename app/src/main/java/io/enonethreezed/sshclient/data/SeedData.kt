package io.enonethreezed.sshclient.data

import io.enonethreezed.sshclient.model.AppData

object SeedData {
    fun create(): AppData {
        return AppData(
            keys = emptyList(),
            profiles = emptyList(),
            workspaces = emptyList(),
            activeWorkspaceId = "",
            selectedTabId = "",
        )
    }
}
