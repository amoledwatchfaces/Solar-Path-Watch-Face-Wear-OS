plugins {
    id ("com.android.application") version ("9.4.0") apply false
    id ("org.jetbrains.kotlin.android") version ("2.4.10") apply false
    id ("org.jetbrains.kotlin.plugin.compose") version ("2.4.10") apply false
    id ("org.jetbrains.kotlin.plugin.parcelize") version ("2.4.10") apply false
    id ("com.google.dagger.hilt.android") version ("2.60.1") apply false
    id ("com.google.devtools.ksp") version ("2.3.4") apply false
}

tasks.register("clean", Delete::class) {
    description = "Clean build directory"
    delete(rootProject.layout.buildDirectory)
}

buildscript {
    /** Set version for wear & watchface modules **/
    extra.set("versionCode", 10000008)
    extra.set("versionName", "1.0.8")
    extra.set("namespace", "com.amoledwatchfaces.solarpath")

    dependencies {
        classpath ("com.android.tools.build:gradle:9.4.1")
        classpath ("org.jetbrains.kotlin:kotlin-serialization:2.4.10")
    }
    repositories {
        google()
    }
}
