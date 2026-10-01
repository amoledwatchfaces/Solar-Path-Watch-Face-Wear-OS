package com.amoledwatchfaces.solarpath.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.amoledwatchfaces.solarpath.utils.formatCoordinate
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

data class AddressInfo(
    val city: String,
    val subName: String
)

interface AddressProvider {
    suspend fun getAddressFromLocation(latitude: Double, longitude: Double): AddressInfo?
}

@Module
@InstallIn(SingletonComponent::class)
class AddressModule {
    @Provides
    fun provideAddressProvider(@ApplicationContext context: Context): AddressProvider {
        return object : AddressProvider {
            override suspend fun getAddressFromLocation(
                latitude: Double,
                longitude: Double
            ): AddressInfo? = suspendCancellableCoroutine { continuation ->
                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= 33) {
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val city = address.locality ?: address.subLocality ?: address.subAdminArea ?: address.featureName ?: "Unknown"
                                val subName = address.adminArea ?: address.countryName ?: ("${formatCoordinate(latitude, true)} ${formatCoordinate(longitude, false)}")
                                continuation.resume(AddressInfo(city, subName))
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            super.onError(errorMessage)
                            continuation.resume(null)
                        }
                    })
                } else {
                    try {
                        @Suppress("DEPRECATION")
                        val addresses: MutableList<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val city = address.locality ?: address.subLocality ?: address.subAdminArea ?: address.featureName ?: "Unknown"
                            val subName = address.adminArea ?: address.countryName ?: ("${formatCoordinate(latitude, true)} ${formatCoordinate(longitude, false)}")
                            continuation.resume(AddressInfo(city, subName))
                        } else {
                            continuation.resume(null)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        continuation.resume(null)
                    }
                }
            }
        }
    }
}
