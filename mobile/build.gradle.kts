plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.kamino.howanyalife.mobile"
    compileSdk = 37

    defaultConfig {
        // Same application ID as the watch app, as is usual for the phone and watch
        // versions of one app; each keeps its own cat.
        applicationId = "com.kamino.howanyalife"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // Signing is set up in the root build.gradle.kts, shared with the watch app.
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    // Glance brings in WorkManager 2.7 (Room 2.2), whose keep rules no longer survive R8 and make
    // release builds crash at startup; a current WorkManager fixes that.
    implementation(libs.androidx.work.runtime)
}
