plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.sakos.camera.capture.camerax"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
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
    api(project(":safety-core"))
    api(libs.androidx.camera.core)
    api(libs.androidx.camera.video)
    api(libs.androidx.camera.lifecycle)
    api(libs.androidx.camera.view)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.drawerlayout:drawerlayout:1.1.1")
    implementation(libs.coroutines.android)
    api(libs.androidx.camera.camera2)
    implementation("androidx.activity:activity:1.9.3")
    api("androidx.lifecycle:lifecycle-livedata:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.collection:collection-ktx:1.4.4")
    implementation("androidx.arch.core:core-runtime:2.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    testImplementation(kotlin("test"))
}
