import org.jetbrains.kotlin.backend.wasm.ir2wasm.bind

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.justplaytvideoplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.justplaytvideoplayer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Media3 (ExoPlayer & UI)
    implementation(libs.androidx.media3.exoplayer.v171)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.ui.v171)
    implementation(libs.androidx.media3.ui.compose)
    // Optional - if you use SmoothStreaming
    implementation(libs.androidx.media3.exoplayer.smoothstreaming)
    implementation("androidx.cardview:cardview:1.0.0")
}