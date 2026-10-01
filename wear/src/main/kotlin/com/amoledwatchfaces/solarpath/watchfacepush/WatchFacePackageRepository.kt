package com.amoledwatchfaces.solarpath.watchfacepush

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.IOException

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
