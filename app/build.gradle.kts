plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.adskip.assist"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.adskip.assist"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // GitHub Releases use a long-lived developer-owned signing key.
    // Missing secrets never fall back to debug signing for release builds.
    val uploadStore = System.getenv("ADSKIP_RELEASE_KEYSTORE_PATH")
    val uploadStorePassword = System.getenv("ADSKIP_RELEASE_STORE_PASSWORD")
    val uploadKeyAlias = System.getenv("ADSKIP_RELEASE_KEY_ALIAS")
    val uploadKeyPassword = System.getenv("ADSKIP_RELEASE_KEY_PASSWORD")
    val hasUploadSigning = listOf(uploadStore, uploadStorePassword, uploadKeyAlias, uploadKeyPassword)
        .all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasUploadSigning) {
            create("githubRelease") {
                storeFile = file(requireNotNull(uploadStore))
                storePassword = requireNotNull(uploadStorePassword)
                keyAlias = requireNotNull(uploadKeyAlias)
                keyPassword = requireNotNull(uploadKeyPassword)
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasUploadSigning) signingConfig = signingConfigs.getByName("githubRelease")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
