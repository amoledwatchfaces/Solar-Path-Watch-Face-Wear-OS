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
