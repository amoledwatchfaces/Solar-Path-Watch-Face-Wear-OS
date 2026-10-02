package com.amoledwatchfaces.solarpath.data

import androidx.compose.runtime.Immutable
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

@Immutable
@Serializable
data class SavedLocation(
    val primaryText: String,
    val secondaryText: String,
    val latitude: Double,
    val longitude: Double
)

@Immutable
@Serializable
data class UserPreferences(
    val backgroundLocationState: Boolean = false,
    val backgroundLocationRepeatInterval: Long = 60,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "- -",
    val locationSubName: String = "",
    val recentLocations: List<SavedLocation> = emptyList(),
    val hideAppFromLauncher: Boolean = false,
)

class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<UserPreferences>
) {
    fun getPreferences() = dataStore.data
}

object UserPreferencesSerializer : Serializer<UserPreferences> {
    override val defaultValue = UserPreferences()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            val json = Json { ignoreUnknownKeys = true }
            return json.decodeFromString(
                UserPreferences.serializer(), input.readBytes().decodeToString()
            )
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read UserPreferences", serialization)
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        withContext(Dispatchers.IO) {
            output.write(
                Json.encodeToString(UserPreferences.serializer(), t)
                    .encodeToByteArray()
            )
        }
    }
}
