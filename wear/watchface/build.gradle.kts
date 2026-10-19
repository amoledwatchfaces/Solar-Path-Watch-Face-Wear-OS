plugins {
    id("com.android.application")
}

val watchFacePkgExt = ".watchfacepush.defaultwatchface"

android {
    namespace = rootProject.extra["namespace"].toString()+watchFacePkgExt
    compileSdk = 37

    defaultConfig {
        applicationId = rootProject.extra["namespace"].toString()+watchFacePkgExt
        targetSdk = 37
        versionName = rootProject.extra["versionName"] as String
    }

    flavorDimensions += "version"
    productFlavors {
        create("wff4") {
            dimension = "version"
            minSdk = 36
            versionNameSuffix = "-defaultwatchface"
            versionCode = 30000 + (rootProject.extra["versionCode"] as Int)
        }
        create("wff5") {
            dimension = "version"
            minSdk = 37
            versionNameSuffix = "-wff5"
            versionCode = 30000 + (rootProject.extra["versionCode"] as Int) + 1
        }
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
