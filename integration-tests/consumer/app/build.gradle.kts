plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.sakos.camera.consumer"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.sakos.camera.consumer"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.0-local"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("org.sakos.camera:safety-core:0.0.0-local")
    implementation("org.sakos.camera:safety-opennsfw2:0.0.0-local")
    implementation("org.sakos.camera:capture-camerax:0.0.0-local")
    implementation("org.sakos.camera:capture-video:0.0.0-local")
}
