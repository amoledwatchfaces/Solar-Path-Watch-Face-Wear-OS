package com.amoledwatchfaces.solarpath.watchfacepush

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException

import androidx.wear.watchfacepush.WatchFacePushManagerFactory
import com.amoledwatchfaces.solarpath.R
import kotlinx.coroutines.withContext

private const val TAG = "WatchFacePackageRepository"

class WatchFacePackageRepository(
    val context: Context
) {
    fun pipeWatchFace(
        scope: CoroutineScope,
        watchFaceData: WatchFaceData
    ): FdPipe {
        val (readFd, writeFd) = ParcelFileDescriptor.createPipe()
        scope.launch(Dispatchers.IO) {
            writeFd.use {
                context.assets.open(watchFaceData.assetPath).use { inStream ->
                    FileOutputStream(writeFd.fileDescriptor).use { outStream ->
                        Log.d(TAG, "before transfer")
                        try {
                            val count = inStream.transferTo(outStream)
                            Log.d(TAG, "Transferred $count bytes")
                        } catch (e: IOException) {
                            Log.i(TAG, "Force closed the pipe: ${e.message}")
                        }
                        Log.d(TAG, "after transfer")
                    }
                }
            }
        }
        return FdPipe(readFd, writeFd)
    }

    suspend fun updateOrInstallDefaultWatchFace(
        scope: CoroutineScope,
        watchFaceData: WatchFaceData = DEFAULT_WATCH_FACE,
        setAsActive: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!WatchFacePushManagerFactory.isSupported()) {
                throw IllegalStateException("Watch Face Push is not supported on this device")
            }
            val wfpManager = WatchFacePushManagerFactory.createWatchFacePushManager(context)
            val response = wfpManager.listWatchFaces()
            val installed = response.installedWatchFaceDetails.filter { it.packageName == watchFaceData.packageName }
            val token = context.getString(R.string.default_wf_token)

            val slotId: String
            if (installed.isNotEmpty()) {
                val existingSlot = installed.first()
                pipeWatchFace(scope, watchFaceData).use { pipe ->
                    wfpManager.updateWatchFace(existingSlot.slotId, pipe.readFd, token)
                }
                slotId = existingSlot.slotId
                Log.i(TAG, "Default watch face updated on existing slot $slotId")
            } else {
                val slot = pipeWatchFace(scope, watchFaceData).use { pipe ->
                    wfpManager.addWatchFace(pipe.readFd, token)
                }
                slotId = slot.slotId
                Log.i(TAG, "Default watch face installed into new slot $slotId")
            }

            if (setAsActive) {
                try {
                    wfpManager.setWatchFaceAsActive(slotId)
                    Log.i(TAG, "Default watch face set as active on slot $slotId")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not set watch face as active: ${e.message}")
                }
            }

            slotId
        }
    }
}

data class FdPipe(
    val readFd: ParcelFileDescriptor,
    private val writeFd: ParcelFileDescriptor
) : AutoCloseable {
    override fun close() {
        Log.d("FdPipe", "Closing pipe")
        readFd.close()
        writeFd.close()
    }
}
