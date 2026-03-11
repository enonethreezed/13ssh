package io.enonethreezed.sshclient.data

import android.content.Context
import io.enonethreezed.sshclient.model.AppData
import io.enonethreezed.sshclient.model.AuthMethod
import io.enonethreezed.sshclient.model.KeySource
import io.enonethreezed.sshclient.model.MachineProfile
import io.enonethreezed.sshclient.model.SshAlgorithm
import io.enonethreezed.sshclient.model.SshKeySpec
import io.enonethreezed.sshclient.model.StartupWorkspace
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val stateFile = appContext.getFileStreamPath("app-state.json")
    private val schemaVersion = 3

    fun load(): AppData {
        if (!stateFile.exists()) {
            return SeedData.create().also(::save)
        }

        return runCatching {
            val json = appContext.openFileInput(stateFile.name).bufferedReader().use { it.readText() }
            fromJson(JSONObject(json))
        }.getOrElse {
            SeedData.create().also(::save)
        }
    }

    fun save(data: AppData) {
        val json = toJson(data).toString(2)
        appContext.openFileOutput(stateFile.name, Context.MODE_PRIVATE).bufferedWriter().use { writer ->
            writer.write(json)
        }
    }

    private fun toJson(data: AppData): JSONObject = JSONObject()
        .put("schemaVersion", schemaVersion)
        .put("activeWorkspaceId", data.activeWorkspaceId)
        .put("selectedTabId", data.selectedTabId)
        .put("keys", JSONArray().apply {
            data.keys.forEach { key ->
                put(
                    JSONObject()
                        .put("id", key.id)
                        .put("name", key.name)
                        .put("algorithm", key.algorithm.name)
                        .put("sizeLabel", key.sizeLabel)
                        .put("source", key.source.name)
                        .put("fingerprint", key.fingerprint)
                        .put("publicKey", key.publicKey)
                        .put("privateKey", key.privateKey)
                )
            }
        })
        .put("profiles", JSONArray().apply {
            data.profiles.forEach { profile ->
                put(
                    JSONObject()
                        .put("id", profile.id)
                        .put("label", profile.label)
                        .put("host", profile.host)
                        .put("port", profile.port)
                        .put("username", profile.username)
                        .put("authMethod", profile.authMethod.name)
                        .put("keyId", profile.keyId)
                        .put("colorSeed", profile.colorSeed)
                )
            }
        })
        .put("workspaces", JSONArray().apply {
            data.workspaces.forEach { workspace ->
                put(
                    JSONObject()
                        .put("id", workspace.id)
                        .put("name", workspace.name)
                        .put("launchOnStartup", workspace.launchOnStartup)
                        .put("profileIds", JSONArray(workspace.profileIds))
                )
            }
        })

    private fun fromJson(json: JSONObject): AppData {
        if (json.optInt("schemaVersion", 0) < schemaVersion) {
            return SeedData.create().copy(keys = loadKeys(json))
        }

        val keys = loadKeys(json)
        val profiles = json.optJSONArray("profiles").orEmpty().map { item ->
            item as JSONObject
            MachineProfile(
                id = item.getString("id"),
                label = item.getString("label"),
                host = item.getString("host"),
                port = item.getInt("port"),
                username = item.getString("username"),
                authMethod = item.optString("authMethod", AuthMethod.KEY.name).let(AuthMethod::valueOf),
                keyId = item.optString("keyId").ifBlank { null },
                colorSeed = item.getLong("colorSeed"),
            )
        }
        val workspaces = json.optJSONArray("workspaces").orEmpty().map { item ->
            item as JSONObject
            StartupWorkspace(
                id = item.getString("id"),
                name = item.getString("name"),
                profileIds = item.getJSONArray("profileIds").orEmpty().map { id -> id.toString() },
                launchOnStartup = item.getBoolean("launchOnStartup"),
            )
        }

        val startup = workspaces.firstOrNull { it.id == json.optString("activeWorkspaceId") }
            ?: workspaces.firstOrNull()

        val selectedTabId = json.optString("selectedTabId").ifBlank {
            startup?.profileIds?.firstOrNull()?.let { "tab-$it" }.orEmpty()
        }

        return AppData(
            keys = keys,
            profiles = profiles,
            workspaces = workspaces,
            activeWorkspaceId = startup?.id.orEmpty(),
            selectedTabId = selectedTabId,
        )
    }

    private fun loadKeys(json: JSONObject): List<SshKeySpec> {
        val keys = json.optJSONArray("keys").orEmpty().map { item ->
            item as JSONObject
            SshKeySpec(
                id = item.getString("id"),
                name = item.getString("name"),
                algorithm = SshAlgorithm.valueOf(item.getString("algorithm")),
                sizeLabel = item.getString("sizeLabel"),
                source = KeySource.valueOf(item.getString("source")),
                fingerprint = item.getString("fingerprint"),
                publicKey = item.getString("publicKey"),
                privateKey = item.getString("privateKey"),
            )
        }
        return keys
    }
}

private fun JSONArray?.orEmpty(): List<Any> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(get(index))
        }
    }
}
