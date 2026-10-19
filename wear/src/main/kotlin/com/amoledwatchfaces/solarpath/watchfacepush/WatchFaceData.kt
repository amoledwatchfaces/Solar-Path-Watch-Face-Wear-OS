package com.amoledwatchfaces.solarpath.watchfacepush

data class WatchFaceData(
    val name: String,
    val assetPath: String,
    val packageName: String,
    val versionCode: Long,
    val slotInfo: WatchFaceSlotInfo? = null
)

data class WatchFaceSlotInfo(
    val packageName: String,
    val slotId: String,
    val versionCode: Long,
    val customVersion: String? = null,
    val isActive: Boolean
)

data class WatchFaceSlots(
    val installedWatchFaces: List<WatchFaceSlotInfo>,
    val unusedSlots: Int
)

val DEFAULT_WATCH_FACE = WatchFaceData(
    name = "default_watchface.apk",
    assetPath = "default_watchface.apk",
    packageName = "com.amoledwatchfaces.solarpath.watchfacepush.defaultwatchface",
    versionCode = 30000001L
)

val SOLARPATH_WFF5_WATCH_FACE = WatchFaceData(
    name = "solarpath_wff5.apk",
    assetPath = "solarpath_wff5.apk",
    packageName = "com.amoledwatchfaces.solarpath.watchfacepush.defaultwatchface",
    versionCode = 30000002L
)
