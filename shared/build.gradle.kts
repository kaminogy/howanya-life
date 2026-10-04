plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

// Code and artwork used by both the watch and the phone app: the cat's needs and behavior,
// persistence, the ViewModel driving the simulation, sprite loading and the launcher icon.
android {
    namespace = "com.kamino.howanyalife.shared"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    buildTypes {
        debug {
            // Debug builds: time runs 60x faster so hunger/sleep cycles can be checked quickly.
            buildConfigField("float", "TIME_SCALE", "60f")
        }
        release {
            buildConfigField("float", "TIME_SCALE", "1f")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)

    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    implementation(libs.compose.foundation)

    testImplementation(libs.junit)
}
