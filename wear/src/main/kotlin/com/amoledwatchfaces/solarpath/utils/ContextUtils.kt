package com.amoledwatchfaces.solarpath.utils

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.amoledwatchfaces.solarpath.complication.SolarPathComplicationService
import kotlin.math.abs

fun formatCoordinate(coordinate: Double, isLatitude: Boolean): String {
    val absoluteValue = abs(coordinate)
    val degrees = absoluteValue.toInt()
    val minutes = ((absoluteValue - degrees) * 60).toInt()

    val direction = if (isLatitude) {
        if (coordinate >= 0) "N" else "S"
    } else {
        if (coordinate >= 0) "E" else "W"
    }

    return "$degrees°$minutes'$direction"
}

fun Context.areLocationPermissionsGranted(): Boolean {
    return isPermissionGranted(android.Manifest.permission.ACCESS_COARSE_LOCATION)
}

fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

fun Context.isOnline(): Boolean {
    val connectivityManager = ContextCompat.getSystemService(this, ConnectivityManager::class.java) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

fun Context.isLocationEnabled(): Boolean {
    val locationManager = ContextCompat.getSystemService(this, LocationManager::class.java) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
}

fun Context.updateComplications() {
    val component = ComponentName(this, SolarPathComplicationService::class.java)
    ComplicationDataSourceUpdateRequester.create(this, component).requestUpdateAll()
}

fun Context.openPlayStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

