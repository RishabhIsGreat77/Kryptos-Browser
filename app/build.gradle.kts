plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kryptos.browser"
    compileSdk = 34

    // NEW: Signing Config for permanent Keystore
    signingConfigs {
        create("release") {
            storeFile = file("kryptos.p12") // Tere file ka exact naam
            storePassword = "Emberlog"        // Jo password tune website par dala tha
            keyAlias = "kryptos"                // Jo alias dala tha
            keyPassword = "Emberlog"          // Jo password dala tha
        }
    }

    defaultConfig {
        applicationId = "com.kryptos.browser"
        minSdk = 26
        targetSdk = 34
        versionCode = 8 
        versionName = "5.0.1" 

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // NEW: Apply the signing config to release
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            // NEW: Apply the same signing config to debug so GitHub Actions uses it
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
