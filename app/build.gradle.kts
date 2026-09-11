import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.premiumenglish.keyboard"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        buildConfig = true
    }

    /*
     * Two editions, built from two source sets.
     *
     * The free edition is this repository. The pro edition adds app/src/pro,
     * which holds the Courtly and Sovereign vocabulary and is not published
     * here — so the free build is not a crippled pro build, it is a build that
     * never had those tables in it.
     *
     * Different application ids, so both can be installed at once.
     */
    flavorDimensions += "edition"

    productFlavors {
        create("free") {
            dimension = "edition"
            applicationId = "com.premiumenglish.keyboard"
            resValue("string", "app_name", "Premium English")
            resValue("string", "ime_name", "Premium English Keyboard")
        }
        create("pro") {
            dimension = "edition"
            applicationId = "com.premiumenglish.keyboard.pro"
            resValue("string", "app_name", "Premium English Pro")
            resValue("string", "ime_name", "Premium English Keyboard (Pro)")
        }
    }

    /*
     * Release signing, for the builds uploaded to itch.io. Put the keystore
     * details in keystore.properties (which is not committed); without it the
     * release build is simply left unsigned rather than failing.
     */
    val keystoreProperties = Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

/*
 * Building the Pro edition without its sources fails deep inside the Kotlin
 * compiler with "Unresolved reference: Edition", which tells nobody anything.
 * Say what is actually missing.
 */
if (!file("src/pro").exists()) {
    // "Pro" followed by a capital is the flavour segment of a task name
    // (assembleProDebug); plain "Pro" also appears in mergeStartupProfile.
    val proTask = Regex("Pro[A-Z]")
    tasks.matching { proTask.containsMatchIn(it.name) }.configureEach {
        doFirst {
            throw GradleException(
                "The Pro edition needs app/src/pro, which is not in this repository. " +
                    "See docs/EDITIONS.md. The free edition builds with assembleFreeDebug."
            )
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    // Runs the keyboard view and settings screen on the JVM, so the parts that
    // need a real Android runtime are still covered by the test suite.
    testImplementation("org.robolectric:robolectric:4.13")
}
