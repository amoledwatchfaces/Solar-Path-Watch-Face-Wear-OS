plugins {
    id("com.android.application")
}

val watchFacePkgExt = ".watchfacepush.defaultwatchface"

android {
    namespace = rootProject.extra["namespace"].toString()+watchFacePkgExt
    compileSdk = 37

    defaultConfig {
        applicationId = rootProject.extra["namespace"].toString()+watchFacePkgExt
        minSdk = 36
        targetSdk = 37
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionName"] as String

        versionNameSuffix = "-defaultwatchface"
        versionCode = 30000 + (versionCode ?: 0)
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            //noinspection NotShrinkingResources
            isShrinkResources = false
        }
        debug {
            // isMinifyEnabled needs to be true for debug build in order for Gradle task to finish properly
            isMinifyEnabled = true
        }
    }

    packaging {
        resources {
            excludes += "kotlin/**"
        }
    }
}
