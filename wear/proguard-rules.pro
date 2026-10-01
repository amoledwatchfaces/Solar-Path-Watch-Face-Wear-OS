# Keep Kotlin Serialization members for DataStore
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
    @kotlinx.serialization.SerialName *;
}

# Keep the data package if you have any data classes here used with serialization
-keep class com.amoledwatchfaces.solarpath.data.** { *; }
