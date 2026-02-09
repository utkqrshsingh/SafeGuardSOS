import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Load local.properties for API keys
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.safeguard.sos"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.safeguard.sos"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()

        testInstrumentationRunner = "com.safeguard.sos.HiltTestRunner"

        // BuildConfig fields
        buildConfigField("String", "BASE_URL", "\"${localProperties.getProperty("BASE_URL", "https://api.safeguardsos.com/")}\"")
        buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY", "")}\"")

        // Manifest placeholders
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")

        // Room schema export
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.expandProjection", "true")
        }

        // Vector drawable support
        vectorDrawables {
            useSupportLibrary = true
        }

        // Multi-dex support
        multiDexEnabled = true

        // Resource configurations
        resourceConfigurations += listOf("en", "hi")
    }

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))

                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"

            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            buildConfigField("Boolean", "ENABLE_CRASHLYTICS", "false")
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")

            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
            buildConfigField("Boolean", "ENABLE_CRASHLYTICS", "true")
        }

        create("staging") {
            initWith(getByName("debug"))
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"

            buildConfigField("String", "BASE_URL", "\"https://staging-api.safeguardsos.com/\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
            buildConfigField("Boolean", "ENABLE_CRASHLYTICS", "true")
        }
    }

    flavorDimensions += "environment"

    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"https://dev-api.safeguardsos.com/\"")
        }

        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.safeguardsos.com/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = rootProject.extra["kotlinCompilerExtensionVersion"] as String
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/*.kotlin_module"
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }

    lint {
        abortOnError = false
        checkDependencies = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        xmlReport = true
        htmlReport = true
        disable += listOf("MissingTranslation", "ExtraTranslation")
    }

    // Split APKs by ABI
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}

dependencies {
    // Core Library Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Kotlin
    implementation(libs.kotlin.stdlib)
    implementation(libs.core.ktx)

    // Coroutines
    implementation(libs.bundles.coroutines)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Navigation
    implementation(libs.bundles.navigation)

    // Room
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.fragment)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.hilt.work.compiler)

    // Paging
    implementation(libs.paging.runtime.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // UI
    implementation(libs.bundles.ui)
    implementation(libs.lottie)
    implementation(libs.glide)
    ksp(libs.glide.compiler)
    implementation(libs.circleimageview)
    implementation(libs.shimmer)

    // Firebase (using BOM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    // Google Play Services
    implementation(libs.play.services.auth)
    implementation(libs.play.services.location)
    implementation(libs.bundles.maps)

    // Networking
    implementation(libs.bundles.networking)

    // Security
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    // Image Processing
    implementation(libs.compressor)
    implementation(libs.ucrop)

    // Permissions
    implementation(libs.dexter)

    // Logging
    implementation(libs.timber)

    // Debug Tools
    debugImplementation(libs.leakcanary)
    debugImplementation(libs.chucker)
    releaseImplementation(libs.chucker.no.op)

    // Unit Testing
    testImplementation(libs.bundles.testing.unit)
    testImplementation(libs.hilt.testing)
    kspTest(libs.hilt.compiler)

    // Android Testing
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(libs.navigation.testing)
    androidTestImplementation(libs.room.testing)
    kspAndroidTest(libs.hilt.compiler)
}

// Task to print version info
tasks.register("printVersionInfo") {
    doLast {
        println("Application ID: com.safeguard.sos")
        println("Version Name: ${libs.versions.versionName.get()}")
        println("Version Code: ${libs.versions.versionCode.get()}")
        println("Min SDK: ${libs.versions.minSdk.get()}")
        println("Target SDK: ${libs.versions.targetSdk.get()}")
        println("Compile SDK: ${libs.versions.compileSdk.get()}")
    }
}
