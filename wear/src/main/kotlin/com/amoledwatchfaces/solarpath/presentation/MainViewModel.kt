package com.amoledwatchfaces.solarpath.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.remote.interactions.RemoteActivityHelper
import androidx.wear.watchfacepush.WatchFacePushManagerFactory
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.amoledwatchfaces.solarpath.R
import com.amoledwatchfaces.solarpath.data.SavedLocation
import com.amoledwatchfaces.solarpath.data.UserPreferences
import com.amoledwatchfaces.solarpath.data.UserPreferencesRepository
import com.amoledwatchfaces.solarpath.location.AddressProvider
import com.amoledwatchfaces.solarpath.location.LocationPrediction
import com.amoledwatchfaces.solarpath.solar.SolarCalculator
import com.amoledwatchfaces.solarpath.solar.SolarData
import com.amoledwatchfaces.solarpath.utils.areLocationPermissionsGranted
import com.amoledwatchfaces.solarpath.utils.formatCoordinate
import com.amoledwatchfaces.solarpath.utils.isOnline
import com.amoledwatchfaces.solarpath.utils.updateComplications
import com.amoledwatchfaces.solarpath.watchfacepush.DEFAULT_WATCH_FACE
import com.amoledwatchfaces.solarpath.watchfacepush.WatchFaceData
import com.amoledwatchfaces.solarpath.watchfacepush.WatchFacePackageRepository
import com.amoledwatchfaces.solarpath.workers.LocationWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.math.abs

private const val TAG = "SolarPathViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<UserPreferences>,
    private val locationClient: FusedLocationProviderClient,
    private val addressProvider: AddressProvider,
    private val packageRepository: WatchFacePackageRepository
) : ViewModel() {

    private val preferencesRepository = UserPreferencesRepository(dataStore)
    val preferences: StateFlow<UserPreferences> = preferencesRepository.getPreferences()
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _loaderState = MutableStateFlow(false)
    val loaderState: StateFlow<Boolean> = _loaderState.asStateFlow()

    private val _searchPredictions = MutableStateFlow<List<LocationPrediction>>(emptyList())
    val searchPredictions: StateFlow<List<LocationPrediction>> = _searchPredictions.asStateFlow()

    private val _pushStatus = MutableStateFlow<String?>(null)
    val pushStatus: StateFlow<String?> = _pushStatus.asStateFlow()

    private val _isWatchFaceActive = MutableStateFlow(false)
    val isWatchFaceActive: StateFlow<Boolean> = _isWatchFaceActive.asStateFlow()

    private val _backgroundLocationDialogState = MutableStateFlow(false)
    val backgroundLocationDialogState: StateFlow<Boolean> = _backgroundLocationDialogState.asStateFlow()

    fun setBackgroundLocationDialogState(state: Boolean) {
        _backgroundLocationDialogState.value = state
    }

    private val _initialLocationDialogState = MutableStateFlow(false)
    val initialLocationDialogState: StateFlow<Boolean> = _initialLocationDialogState.asStateFlow()

    fun setInitialLocationDialogState(state: Boolean) {
        _initialLocationDialogState.value = state
    }

    private val _refreshTrigger = kotlinx.coroutines.flow.MutableStateFlow(System.currentTimeMillis())

    fun refresh(forceRecalculate: Boolean = false) {
        if (forceRecalculate) {
            SolarCalculator.invalidateCache()
        }
        _refreshTrigger.value = System.currentTimeMillis()
    }

    private val timeTickerFlow = kotlinx.coroutines.flow.flow {
        while (true) {
            emit(System.currentTimeMillis())
            kotlinx.coroutines.delay(30_000L)
        }
    }

    val solarData: StateFlow<SolarData> = combine(
        preferences,
        _refreshTrigger,
        timeTickerFlow
    ) { prefs, refreshMs, tickerMs ->
        val nowMs = maxOf(refreshMs, tickerMs)
        SolarCalculator.calculateSolarData(
            lat = prefs.latitude,
            lon = prefs.longitude,
            date = java.time.LocalDate.now(),
            currentTimeMillis = nowMs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SolarData())

    val defaultWatchFace = DEFAULT_WATCH_FACE

    init {
        checkWatchFaceActiveStatus()
    }

    fun checkWatchFaceActiveStatus() {
        if (!WatchFacePushManagerFactory.isSupported()) return
        viewModelScope.launch {
            try {
                val wfpManager = WatchFacePushManagerFactory.createWatchFacePushManager(context)
                val isActive = wfpManager.isWatchFaceActive(defaultWatchFace.packageName)
                _isWatchFaceActive.value = isActive
            } catch (e: Exception) {
                Log.e(TAG, "Error checking active status: ${e.message}")
            }
        }
    }

    fun pushDefaultWatchFace() {
        if (!WatchFacePushManagerFactory.isSupported()) {
            Toast.makeText(context, R.string.wfp_not_supported, Toast.LENGTH_LONG).show()
            return
        }

        viewModelScope.launch {
            _loaderState.value = true
            try {
                val result = packageRepository.updateOrInstallDefaultWatchFace(
                    scope = this,
                    watchFaceData = defaultWatchFace,
                    setAsActive = true
                )
                if (result.isSuccess) {
                    val wfpManager = WatchFacePushManagerFactory.createWatchFacePushManager(context)
                    val isActive = wfpManager.isWatchFaceActive(defaultWatchFace.packageName)
                    _isWatchFaceActive.value = isActive
                    if (isActive) {
                        Toast.makeText(context, R.string.watch_face_active, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, R.string.status_manual_activation, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, R.string.status_manual_activation, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Watch Face Push error: ${e.message}", e)
                Toast.makeText(context, "Push failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _loaderState.value = false
                checkWatchFaceActiveStatus()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        if (!context.isOnline()) {
            Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_SHORT).show()
            return
        }

        if (context.areLocationPermissionsGranted()) {
            _loaderState.value = true
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setGranularity(Granularity.GRANULARITY_COARSE)
                .build()

            locationClient.getCurrentLocation(currentLocationRequest, object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token
                override fun isCancellationRequested() = false
            })
                .addOnSuccessListener { loc ->
                    if (loc == null) {
                        _loaderState.value = false
                        Toast.makeText(context, R.string.no_location, Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        viewModelScope.launch {
                            val addressInfo = addressProvider.getAddressFromLocation(loc.latitude, loc.longitude)
                            val cityName = addressInfo?.city ?: "Unknown"
                            val subName = addressInfo?.subName ?: "${formatCoordinate(loc.latitude, true)} ${formatCoordinate(loc.longitude, false)}"
                            val savedLoc = if (cityName != "Unknown" && cityName != "- -") SavedLocation(cityName, subName, loc.latitude, loc.longitude) else null

                            dataStore.updateData { prefs ->
                                val updatedRecent = if (savedLoc != null) {
                                    listOf(savedLoc) + prefs.recentLocations.filterNot { item ->
                                        (abs(item.latitude - savedLoc.latitude) < 0.001 && abs(item.longitude - savedLoc.longitude) < 0.001) ||
                                                (item.primaryText == savedLoc.primaryText && item.secondaryText == savedLoc.secondaryText)
                                    }
                                } else prefs.recentLocations

                                prefs.copy(
                                    latitude = loc.latitude,
                                    longitude = loc.longitude,
                                    locationName = cityName,
                                    locationSubName = subName,
                                    recentLocations = updatedRecent.take(10)
                                )
                            }
                            context.updateComplications()
                            _loaderState.value = false
                        }
                    }
                }
                .addOnFailureListener {
                    _loaderState.value = false
                    Toast.makeText(context, "Location request failed", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun searchLocation(query: String, callback: (Boolean) -> Unit) {
        if (query.isBlank()) {
            callback(false)
            return
        }

        _loaderState.value = true
        viewModelScope.launch {
            val predictions = fetchLocations(query)
            _loaderState.value = false
            if (predictions.isEmpty()) {
                Toast.makeText(context, "No results found", Toast.LENGTH_LONG).show()
            }
            _searchPredictions.value = predictions
            callback.invoke(predictions.isNotEmpty())
        }
    }

    private suspend fun fetchLocations(query: String): List<LocationPrediction> = suspendCancellableCoroutine { continuation ->
        val geocoder = Geocoder(context, Locale.getDefault())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 5, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    val mapped = addresses.map { address ->
                        val name = address.locality ?: address.subAdminArea ?: address.featureName ?: query
                        val adminArea = address.adminArea ?: address.countryName ?: ""
                        LocationPrediction(name, adminArea, address.latitude, address.longitude)
                    }
                    if (continuation.isActive) continuation.resume(mapped)
                }

                override fun onError(errorMessage: String?) {
                    if (continuation.isActive) continuation.resume(emptyList())
                }
            })
        } else {
            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocationName(query, 5)
                val mapped = addresses?.map { address ->
                    val name = address.locality ?: address.subAdminArea ?: address.featureName ?: query
                    val adminArea = address.adminArea ?: address.countryName ?: ""
                    LocationPrediction(name, adminArea, address.latitude, address.longitude)
                } ?: emptyList()
                continuation.resume(mapped)
            } catch (e: Exception) {
                continuation.resume(emptyList())
            }
        }
    }

    fun getLocationCoordinates(prediction: LocationPrediction) {
        _loaderState.value = true
        val savedLoc = SavedLocation(prediction.primaryText, prediction.secondaryText, prediction.latitude, prediction.longitude)

        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val updatedRecent = listOf(savedLoc) + prefs.recentLocations.filterNot { item ->
                    (abs(item.latitude - savedLoc.latitude) < 0.001 && abs(item.longitude - savedLoc.longitude) < 0.001) ||
                            (item.primaryText == savedLoc.primaryText && item.secondaryText == savedLoc.secondaryText)
                }
                prefs.copy(
                    latitude = prediction.latitude,
                    longitude = prediction.longitude,
                    locationName = prediction.primaryText,
                    locationSubName = prediction.secondaryText,
                    recentLocations = updatedRecent.take(10)
                )
            }
            context.updateComplications()
            _loaderState.value = false
        }
    }

    fun selectSavedLocation(location: SavedLocation) {
        _loaderState.value = true
        val savedLoc = SavedLocation(location.primaryText, location.secondaryText, location.latitude, location.longitude)
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val updatedRecent = listOf(savedLoc) + prefs.recentLocations.filterNot { item ->
                    (abs(item.latitude - savedLoc.latitude) < 0.001 && abs(item.longitude - savedLoc.longitude) < 0.001) ||
                            (item.primaryText == savedLoc.primaryText && item.secondaryText == savedLoc.secondaryText)
                }
                prefs.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    locationName = location.primaryText,
                    locationSubName = location.secondaryText,
                    recentLocations = updatedRecent.take(10)
                )
            }
            context.updateComplications()
            _loaderState.value = false
        }
    }

    fun removeSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            dataStore.updateData { prefs ->
                val updatedList = prefs.recentLocations.filterNot { item ->
                    (abs(item.latitude - location.latitude) < 0.001 && abs(item.longitude - location.longitude) < 0.001) ||
                            (item.primaryText == location.primaryText && item.secondaryText == location.secondaryText)
                }
                prefs.copy(recentLocations = updatedList)
            }
        }
    }

    fun setBackgroundLocation(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.updateData { it.copy(backgroundLocationState = enabled) }
            if (enabled) {
                LocationWorker.scheduleBackgroundLocation(context, preferences.value.backgroundLocationRepeatInterval)
            } else {
                LocationWorker.unregisterPassiveLocationUpdates(context)
            }
        }
    }

    fun setBackgroundLocationRepeatInterval(interval: Long) {
        viewModelScope.launch {
            dataStore.updateData { it.copy(backgroundLocationRepeatInterval = interval) }
            if (preferences.value.backgroundLocationState) {
                LocationWorker.scheduleBackgroundLocation(context, interval)
            }
        }
    }

    fun openLinkOnPhone(link: String, onOpened: (Boolean) -> Unit) {
        viewModelScope.launch {
            val remoteActivityHelper = RemoteActivityHelper(context)
            val intent = Intent(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(link.toUri())
            try {
                remoteActivityHelper.startRemoteActivity(targetIntent = intent, targetNodeId = null).await()
                onOpened(true)
            } catch (cancellationException: CancellationException) {
                Log.e(TAG, "$cancellationException")
            } catch (throwable: Throwable) {
                Log.e(TAG, "$throwable")
                onOpened(false)
            }
        }
    }
}
