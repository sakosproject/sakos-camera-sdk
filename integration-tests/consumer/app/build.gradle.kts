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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testProguardFiles("test-proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        create("localRuntime") {
            initWith(getByName("release"))
            proguardFiles("local-runtime-test-support.pro")
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            versionNameSuffix = "-synthetic-smoke"
        }
    }
    testBuildType = "localRuntime"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.collection:collection-ktx:1.4.4")
    implementation("androidx.arch.core:core-runtime:2.2.0")
    implementation("org.sakos.camera:safety-core:0.0.0-local")
    implementation("org.sakos.camera:safety-opennsfw2:0.0.0-local")
    implementation("org.sakos.camera:capture-camerax:0.0.0-local")
    implementation("org.sakos.camera:capture-video:0.0.0-local")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
}
