package com.zcodemobile.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "zcode_mobile")

data class SavedConnection(
    val id: String,
    val url: String,
    val host: String,
    val name: String,
    val lastUsed: Long,
    val favorite: Boolean = false,
)

data class AppSettings(
    val autoReconnect: Boolean = true,
    val keepScreenOn: Boolean = true,
    val themeMode: Int = 0,
)

// 配对 URL 内嵌口令哈希，等同钥匙，只存应用私有目录
class ConnectionStore(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val connections: Flow<List<SavedConnection>> = dataStore.data.map { prefs ->
        decodeConnections(prefs[KEY_CONNECTIONS])
    }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            autoReconnect = prefs[KEY_AUTO_RECONNECT] ?: true,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: true,
            themeMode = prefs[KEY_THEME_MODE] ?: 0,
        )
    }

    suspend fun upsert(connection: ZcodeConnection) {
        dataStore.edit { prefs ->
            val list = decodeConnections(prefs[KEY_CONNECTIONS]).toMutableList()
            val existing = list.indexOfFirst { it.url == connection.url }
            val entry = SavedConnection(
                id = if (existing >= 0) list[existing].id else connection.url.hashCode().toString(16) + System.nanoTime().toString(16),
                url = connection.url,
                host = connection.host,
                name = list.getOrNull(existing)?.takeIf { it.name != connection.host && it.name.isNotBlank() }?.name
                    ?: QRUrlParser.displayName(connection),
                lastUsed = System.currentTimeMillis(),
                favorite = list.getOrNull(existing)?.favorite ?: false,
            )
            if (existing >= 0) list[existing] = entry else list.add(entry)
            prefs[KEY_CONNECTIONS] = encodeConnections(list)
        }
    }

    suspend fun rename(id: String, name: String) {
        dataStore.edit { prefs ->
            val list = decodeConnections(prefs[KEY_CONNECTIONS]).toMutableList()
            val i = list.indexOfFirst { it.id == id }
            if (i >= 0) {
                list[i] = list[i].copy(name = name)
                prefs[KEY_CONNECTIONS] = encodeConnections(list)
            }
        }
    }

    suspend fun setFavorite(id: String, favorite: Boolean) {
        dataStore.edit { prefs ->
            val list = decodeConnections(prefs[KEY_CONNECTIONS]).toMutableList()
            val i = list.indexOfFirst { it.id == id }
            if (i >= 0) {
                list[i] = list[i].copy(favorite = favorite)
                prefs[KEY_CONNECTIONS] = encodeConnections(list)
            }
        }
    }

    suspend fun touch(id: String) {
        dataStore.edit { prefs ->
            val list = decodeConnections(prefs[KEY_CONNECTIONS]).toMutableList()
            val i = list.indexOfFirst { it.id == id }
            if (i >= 0) {
                list[i] = list[i].copy(lastUsed = System.currentTimeMillis())
                prefs[KEY_CONNECTIONS] = encodeConnections(list)
            }
        }
    }

    suspend fun remove(id: String) {
        dataStore.edit { prefs ->
            val list = decodeConnections(prefs[KEY_CONNECTIONS]).filterNot { it.id == id }
            prefs[KEY_CONNECTIONS] = encodeConnections(list)
        }
    }

    suspend fun setAutoReconnect(value: Boolean) = dataStore.edit { it[KEY_AUTO_RECONNECT] = value }
    suspend fun setKeepScreenOn(value: Boolean) = dataStore.edit { it[KEY_KEEP_SCREEN_ON] = value }
    suspend fun setThemeMode(value: Int) = dataStore.edit { it[KEY_THEME_MODE] = value }

    companion object {
        private val KEY_CONNECTIONS = stringPreferencesKey("connections")
        private val KEY_AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
    }
}

internal fun decodeConnections(json: String?): List<SavedConnection> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val url = o.optString("url")
            if (url.isBlank()) return@mapNotNull null
            SavedConnection(
                id = o.optString("id"),
                url = url,
                host = o.optString("host"),
                name = o.optString("name").ifBlank { o.optString("host") },
                lastUsed = o.optLong("lastUsed"),
                favorite = o.optBoolean("favorite"),
            )
        }
    }.getOrDefault(emptyList())
}

internal fun encodeConnections(list: List<SavedConnection>): String {
    val arr = JSONArray()
    list.forEach { c ->
        arr.put(JSONObject().apply {
            put("id", c.id)
            put("url", c.url)
            put("host", c.host)
            put("name", c.name)
            put("lastUsed", c.lastUsed)
            put("favorite", c.favorite)
        })
    }
    return arr.toString()
}
