import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    // AGP 9 has built-in Kotlin; this only pins the Kotlin Gradle Plugin version on the classpath.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Both apps are release-signed with the same key: the Data Layer only connects apps signed alike,
// and a published APK can only be updated by one signed with the same key.
// CI passes the key through RELEASE_KEYSTORE* environment variables; locally, local.properties
// points `releaseSigning` at a properties file (storeFile, storePassword, keyAlias, keyPassword).
// Without either, release builds fall back to this machine's debug key (but never on CI).
val releaseSigning: Map<String, String>? = run {
    System.getenv("RELEASE_KEYSTORE")?.let { storeFile ->
        return@run mapOf(
            "storeFile" to storeFile,
            "storePassword" to System.getenv("RELEASE_KEYSTORE_PASSWORD"),
            "keyAlias" to System.getenv("RELEASE_KEY_ALIAS"),
            "keyPassword" to System.getenv("RELEASE_KEY_PASSWORD"),
        )
    }
    val localProps = file("local.properties").takeIf { it.exists() }
        ?.let { f -> Properties().apply { f.inputStream().use { load(it) } } }
    val signingFile = localProps?.getProperty("releaseSigning")?.let(::file) ?: return@run null
    Properties().apply { signingFile.inputStream().use { load(it) } }
        .let { p -> p.stringPropertyNames().associateWith { p.getProperty(it) } }
}

if (releaseSigning == null && System.getenv("CI") != null) {
    throw GradleException("RELEASE_KEYSTORE is not set; refusing to sign a CI release with a debug key")
}

subprojects {
    pluginManager.withPlugin("com.android.application") {
        extensions.configure<ApplicationExtension> {
            val release = releaseSigning?.let { s ->
                signingConfigs.create("release") {
                    storeFile = file(s.getValue("storeFile"))
                    storePassword = s.getValue("storePassword")
                    keyAlias = s.getValue("keyAlias")
                    keyPassword = s.getValue("keyPassword")
                }
            } ?: signingConfigs.getByName("debug")
            buildTypes.getByName("release").signingConfig = release
        }
    }
}
