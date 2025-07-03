// app/build.gradle.kts

plugins {
    // Version-catalog aliases (ensure these are defined in libs.versions.toml)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // KAPT for annotation processors
    id("kotlin-kapt")

    // Hilt Gradle plugin
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.example.positionme2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.positionme2"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add BuildConfig field for OPENPOSITIONING_MASTER_KEY
        buildConfigField("String", "OPENPOSITIONING_MASTER_KEY", "\"your_master_key_here\"")
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

    buildFeatures {
        compose = true
        buildConfig = true  // Enable BuildConfig generation
    }
    composeOptions {
        // Hard-code the compiler extension version to match your Compose setup
        kotlinCompilerExtensionVersion = "1.5.10"
    }
}

dependencies {
    // AndroidX core & lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Compose BOM and UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Material Design 3 for XML layouts
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")
    // Traditional Navigation Component
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Material Icons (optional)
    implementation("androidx.compose.material:material-icons-core:1.6.6")
    implementation("androidx.compose.material:material-icons-extended:1.6.6")

    // Maps Compose + Play Services Maps
    implementation("com.google.maps.android:maps-compose:2.12.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Google Play Services Location for system GPS
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Hilt for DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // Hilt + Compose Navigation integration
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // LiveData for Compose
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7")

    // JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Network for API communication - using basic Java libraries only
    implementation("org.json:json:20230618")

    // Compose Material (classic)
    implementation("androidx.compose.material:material:1.6.7")
}
