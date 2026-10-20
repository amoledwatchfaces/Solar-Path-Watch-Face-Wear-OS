import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.util.Properties
import java.util.regex.Pattern

evaluationDependsOn(":wear:watchface")

plugins {
    id ("com.android.application")
    id ("kotlinx-serialization")
    id ("com.google.devtools.ksp")
    id ("com.google.dagger.hilt.android")
    id ("org.jetbrains.kotlin.plugin.compose")
    id ("org.jetbrains.kotlin.plugin.parcelize")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

// Watch Face Push requires API level 36 and above.
android {
    namespace = rootProject.extra["namespace"] as String

    compileSdk = 37

    defaultConfig {
        applicationId = rootProject.extra["namespace"] as String
        targetSdk = 37
        versionName = rootProject.extra["versionName"] as String
    }

    flavorDimensions += "version"
    productFlavors {
        create("wff4") {
            dimension = "version"
            minSdk = 36
            versionCode = 20000 + (rootProject.extra["versionCode"] as Int)
            versionNameSuffix = "-wff4"
        }
        create("wff5") {
            dimension = "version"
            minSdk = 37
            versionCode = 30000 + (rootProject.extra["versionCode"] as Int)
            versionNameSuffix = "-wff5"
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFileEnv = System.getenv("KEYSTORE_FILE") ?: System.getenv("KEYSTORE_FILE_PATH")
            val keystorePasswordEnv = System.getenv("KEYSTORE_PASSWORD")
            val keyAliasEnv = System.getenv("KEY_ALIAS")
            val keyPasswordEnv = System.getenv("KEY_PASSWORD")

            if (!keystoreFileEnv.isNullOrEmpty() && !keystorePasswordEnv.isNullOrEmpty() && !keyAliasEnv.isNullOrEmpty() && !keyPasswordEnv.isNullOrEmpty()) {
                storeFile = file(keystoreFileEnv)
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            } else {
                val candidateLocations = listOf(
                    file("C:\\Users\\amoledwatchfaces\\workspace\\keystore"),
                    file("C:\\Users\\amoledwatchfaces\\WatchFaceStudio\\keystore")
                )
                val targetDir = candidateLocations.firstOrNull {
                    File(it, "keystore.properties").exists() && File(it, "keystore.jks").exists()
                }
                if (targetDir != null) {
                    val localPropertiesFile = File(targetDir, "keystore.properties")
                    val localKeystoreFile = File(targetDir, "keystore.jks")
                    val keystoreProperties = Properties().apply {
                        load(FileInputStream(localPropertiesFile))
                    }
                    storeFile = localKeystoreFile
                    keyAlias = keystoreProperties.getProperty("KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias")
                    storePassword = keystoreProperties.getProperty("STORE_PASSWORD")
                        ?: keystoreProperties.getProperty("KEYSTORE_PASSWORD")
                        ?: keystoreProperties.getProperty("storePassword")
                    keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
                        ?: keystoreProperties.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.getByName("release")
            if (releaseConfig.storeFile != null && releaseConfig.storeFile!!.exists()) {
                signingConfig = releaseConfig
            }
        }
        debug {
            versionNameSuffix = " (debug)"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

configurations {
    create("cliToolConfiguration") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
}

dependencies {
    "cliToolConfiguration"("com.google.android.wearable.watchface.validator:validator-push-cli:1.1.0-alpha01")

    // Wear OS & Watch Face Push
    implementation ("androidx.wear:wear:1.4.0")
    implementation ("androidx.wear:wear-remote-interactions:1.2.0")
    implementation ("androidx.wear.watchfacepush:watchfacepush:1.0.0")
    implementation ("com.google.android.gms:play-services-wearable:20.0.1")
    compileOnly ("com.google.android.wearable:wearable:2.9.0")

    // Complications
    implementation ("androidx.wear.watchface:watchface-complications-data-source-ktx:1.3.0")
    implementation ("androidx.wear.protolayout:protolayout-proto:1.4.2")
    implementation ("androidx.wear.protolayout:protolayout-external-protobuf:1.4.2")

    // Wear OS Widgets & Remote Compose
    implementation ("androidx.compose.remote:remote-creation-compose:1.0.0-alpha21")
    implementation ("androidx.compose.remote:remote-core:1.0.0-alpha21")
    implementation ("androidx.glance.wear:wear:1.0.0-alpha20")
    implementation ("androidx.glance.wear:wear-core:1.0.0-alpha20")
    implementation ("androidx.wear.compose.remote:remote-material3:1.0.0-alpha13")

    // Tiles & ProtoLayout
    implementation ("androidx.wear.tiles:tiles:1.6.2")
    implementation ("androidx.wear.protolayout:protolayout:1.4.2")
    implementation ("androidx.wear.protolayout:protolayout-material3:1.4.2")

    // Tooling for previews
    implementation ("androidx.glance.wear:wear-tooling-preview:1.0.0-alpha20")
    implementation ("androidx.compose.remote:remote-tooling-preview:1.0.0-alpha18")
    implementation ("androidx.wear.tiles:tiles-tooling-preview:1.6.2")

    // Astronomy Engine (Kastro)
    implementation ("dev.jamesyox:kastro:0.6.0")

    // Location
    implementation ("com.google.android.gms:play-services-location:21.4.0")
    implementation ("com.google.accompanist:accompanist-permissions:0.37.3")

    // General compose dependencies
    val composeBom = platform ("androidx.compose:compose-bom:2026.05.01")
    implementation (composeBom)
    implementation ("androidx.activity:activity-compose:1.13.0")
    implementation ("androidx.compose.ui:ui:1.12.1")
    implementation ("androidx.compose.ui:ui-tooling-preview:1.12.1")
    implementation ("androidx.compose.material:material-icons-extended:1.7.8")
    implementation ("androidx.compose.animation:animation-graphics")

    // Compose for Wear OS
    implementation ("androidx.wear.compose:compose-material3:1.7.1")
    implementation ("androidx.wear.compose:compose-navigation:1.7.1")
    implementation ("androidx.wear.compose:compose-foundation:1.7.1")
    implementation ("androidx.wear.compose:compose-ui-tooling:1.7.1")

    // Input / Material3
    implementation ("androidx.compose.material3:material3:1.4.0")

    // Core
    implementation ("androidx.core:core-ktx:1.19.1")
    implementation ("androidx.core:core-splashscreen:1.2.0")

    // Lifecycle
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")

    // Coroutines
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.11.0")

    // DataStore & Serialization
    implementation ("androidx.datastore:datastore:1.2.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // Hilt
    implementation ("androidx.hilt:hilt-navigation-compose:1.4.0")
    implementation ("androidx.hilt:hilt-work:1.4.0")
    implementation ("com.google.dagger:hilt-android:2.60.1")
    ksp ("com.google.dagger:hilt-compiler:2.60.1")
    ksp ("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")
    ksp ("androidx.hilt:hilt-compiler:1.4.0")

    // WorkManager
    implementation ("androidx.work:work-runtime-ktx:2.12.0")

    // Testing
    testImplementation ("junit:junit:4.13.2")
    debugImplementation ("androidx.compose.ui:ui-tooling:1.12.1")
    debugImplementation ("androidx.compose.ui:ui-test-manifest:1.12.1")
}

androidComponents.onVariants { variant ->
    val capsVariant = variant.name.replaceFirstChar { it.uppercase() }
    val flavorName = variant.flavorName ?: "wff4"
    val capsFlavor = flavorName.replaceFirstChar { it.uppercase() }
    val buildType = variant.buildType?.replaceFirstChar { it.uppercase() } ?: "Debug"

    val copyTaskProvider = tasks.register<Copy>("copyWatchface${capsVariant}Output") {
        description = "Copies watchface APK into assets for $variant"
        val packageWatchfaceTask = project(":wear:watchface").tasks.named("package${capsFlavor}${buildType}")

        from(packageWatchfaceTask) {
            include("**/*.apk")
            rename { "default_watchface.apk" }
        }
        into(layout.buildDirectory.dir("intermediates/watchfaceAssets/${variant.name}"))

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        includeEmptyDirs = false
    }

    val tokenTask = tasks.register<ProcessFilesTask>("generateToken${capsVariant}Res") {
        description = "Generates token resource for $variant"
        val resDir = layout.buildDirectory.dir("generated/wfTokenRes/${variant.name}/res")
        val assetsDir = layout.buildDirectory.dir("generated/wfAssets/${variant.name}")
        val tokenFile = resDir.map { it.file("values/wf_token.xml") }

        inputAssets.from(copyTaskProvider)
        apkDirectory.set(assetsDir)
        resDirectory.set(resDir)
        outputFile.set(tokenFile)
        cliToolClasspath.set(project.configurations["cliToolConfiguration"])
        rootPackage.set(rootProject.extra["namespace"].toString())
    }

    variant.sources.assets!!.addGeneratedSourceDirectory(tokenTask) { task -> task.apkDirectory }
    variant.sources.res!!.addGeneratedSourceDirectory(tokenTask) { task -> task.resDirectory }
}

abstract class ProcessFilesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    abstract val inputAssets: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val apkDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val resDirectory: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    abstract val rootPackage: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cliToolClasspath: Property<FileCollection>

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun taskAction() {
        val defaultApk = inputAssets.asFileTree.files.find { it.name == "default_watchface.apk" }
            ?: throw GradleException("default_watchface.apk not found in inputAssets. Files: ${inputAssets.asFileTree.files.joinToString()}")

        // Copy APK to apkDirectory
        val targetDefaultFile = apkDirectory.get().file("default_watchface.apk").asFile
        targetDefaultFile.parentFile.mkdirs()
        defaultApk.copyTo(targetDefaultFile, overwrite = true)

        fun generateToken(apk: File): String {
            val stdOut = ByteArrayOutputStream()
            val stdErr = ByteArrayOutputStream()

            execOperations.javaexec {
                classpath = cliToolClasspath.get()
                mainClass = "com.google.android.wearable.watchface.validator.cli.DwfValidation"

                args(
                    "--apk_path=${apk.absolutePath}",
                    "--package_name=${rootPackage.get()}",
                )
                standardOutput = stdOut
                errorOutput = stdErr
                isIgnoreExitValue = true
            }

            val outputAsText = stdOut.toString()
            val errorAsText = stdErr.toString()

            if (outputAsText.contains("Failed check")) {
                println(outputAsText)
                if (errorAsText.isNotEmpty()) {
                    println(errorAsText)
                }
                throw GradleException("Watch face validation failed for ${apk.name}")
            }

            val match = Pattern.compile("generated token: (\\S+)").matcher(outputAsText)
            if (match.find()) {
                return match.group(1)
            } else {
                throw TaskExecutionException(
                    this,
                    GradleException("No token generated for watch face ${apk.name}! Output: $outputAsText Error: $errorAsText"),
                )
            }
        }

        val defaultToken = generateToken(defaultApk)

        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        val tokenResText = """<resources>
                     |    <string name="default_wf_token">$defaultToken</string>
                     |</resources>
                   """.trimMargin()
        output.writeText(tokenResText)
    }
}
