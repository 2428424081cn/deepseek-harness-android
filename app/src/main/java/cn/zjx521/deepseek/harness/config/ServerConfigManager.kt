package cn.zjx521.deepseek.harness.config

import android.content.Context
import android.content.SharedPreferences
import cn.zjx521.deepseek.harness.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Represents a saved server configuration profile.
 */
data class ServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "默认电脑",
    val host: String = "192.168.1.100",
    val port: Int = 3080,
    val useSsl: Boolean = false,
    val lastUsed: Long = System.currentTimeMillis()
) {
    val url: String
        get() = "${if (useSsl) "https" else "http"}://$host:$port"

    val displayAddress: String
        get() = "$host:$port"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("label", label)
        put("host", host)
        put("port", port)
        put("useSsl", useSsl)
        put("lastUsed", lastUsed)
    }

    companion object {
        fun fromJson(json: JSONObject, defaultLabel: String): ServerConfig = ServerConfig(
            id = json.optString("id", UUID.randomUUID().toString()),
            label = json.optString("label", defaultLabel),
            host = json.optString("host", "192.168.1.100"),
            port = json.optInt("port", 3080),
            useSsl = json.optBoolean("useSsl", false),
            lastUsed = json.optLong("lastUsed", System.currentTimeMillis())
        )
    }
}

/**
 * Manages multiple server configurations and remembers the active one.
 */
class ServerConfigManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val defaultLabel: String = context.getString(R.string.default_device)
    private val unnamedLabel: String = context.getString(R.string.unnamed_device)

    /** Returns all saved servers, sorted by last used descending. */
    fun getAllServers(): List<ServerConfig> {
        val jsonString = prefs.getString(KEY_SERVERS_JSON, null) ?: return emptyList()
        val list = mutableListOf<ServerConfig>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                list.add(ServerConfig.fromJson(array.getJSONObject(i), unnamedLabel))
            }
        } catch (_: Exception) {
            // Fallback if parsing fails
        }
        return list.sortedByDescending { it.lastUsed }
    }

    /** Returns the currently active server configuration. */
    var config: ServerConfig
        get() {
            val servers = getAllServers()
            val activeId = prefs.getString(KEY_ACTIVE_SERVER_ID, null)
            val matched = servers.find { it.id == activeId }
            if (matched != null) return matched
            if (servers.isNotEmpty()) return servers.first()

            // Legacy fallback if old single-server format exists
            val legacyHost = prefs.getString(KEY_LEGACY_HOST, null)
            if (legacyHost != null) {
                val legacy = ServerConfig(
                    label = prefs.getString(KEY_LEGACY_LABEL, defaultLabel) ?: defaultLabel,
                    host = legacyHost,
                    port = prefs.getInt(KEY_LEGACY_PORT, 3080),
                    useSsl = prefs.getBoolean(KEY_LEGACY_SSL, false)
                )
                saveServer(legacy, makeActive = true)
                return legacy
            }

            return ServerConfig(label = defaultLabel)
        }
        set(value) {
            saveServer(value, makeActive = true)
        }

    /** Saves or updates a server profile. */
    fun saveServer(server: ServerConfig, makeActive: Boolean = true) {
        val servers = getAllServers().toMutableList()
        val index = servers.indexOfFirst { it.id == server.id }
        val updated = server.copy(lastUsed = System.currentTimeMillis())

        if (index >= 0) {
            servers[index] = updated
        } else {
            servers.add(0, updated)
        }

        saveServersList(servers)

        if (makeActive) {
            prefs.edit().putString(KEY_ACTIVE_SERVER_ID, updated.id).apply()
        }
    }

    /** Switches the active server by ID. */
    fun setActiveServer(id: String) {
        val servers = getAllServers().toMutableList()
        val index = servers.indexOfFirst { it.id == id }
        if (index >= 0) {
            val updated = servers[index].copy(lastUsed = System.currentTimeMillis())
            servers[index] = updated
            saveServersList(servers)
            prefs.edit().putString(KEY_ACTIVE_SERVER_ID, id).apply()
        }
    }

    /** Deletes a server profile by ID. */
    fun deleteServer(id: String) {
        val servers = getAllServers().filter { it.id != id }
        saveServersList(servers)
        if (prefs.getString(KEY_ACTIVE_SERVER_ID, null) == id) {
            val next = servers.firstOrNull()
            prefs.edit().putString(KEY_ACTIVE_SERVER_ID, next?.id).apply()
        }
    }

    /** True if at least one server has been saved. */
    val isConfigured: Boolean
        get() = getAllServers().isNotEmpty() || prefs.contains(KEY_LEGACY_HOST)

    private fun saveServersList(servers: List<ServerConfig>) {
        val array = JSONArray()
        servers.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SERVERS_JSON, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "dsh_server_config"
        private const val KEY_SERVERS_JSON = "servers_json"
        private const val KEY_ACTIVE_SERVER_ID = "active_server_id"

        // Legacy keys for migration
        private const val KEY_LEGACY_LABEL = "label"
        private const val KEY_LEGACY_HOST = "host"
        private const val KEY_LEGACY_PORT = "port"
        private const val KEY_LEGACY_SSL = "ssl"
    }
}
