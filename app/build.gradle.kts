plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.comicreader.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.comicreader.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.github.junrar:junrar:7.5.4")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
