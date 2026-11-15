import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Optional image-API keys live in local.properties (never committed). Blank = source disabled.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localKey(name: String): String = (localProps.getProperty(name) ?: "").replace("\"", "")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    // Build JDK is 25; keep bytecode at 17 so Java and Kotlin agree (Android TV 11 runs it fine).
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.arjunvision.tvlauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.arjunvision.tvlauncher"
        // Android TV 11 (the Mi TV MiTV-M00R2) is API 30.
        minSdk = 30
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        buildConfigField("String", "PEXELS_API_KEY", "\"${localKey("PEXELS_API_KEY")}\"")
        buildConfigField("String", "PIXABAY_API_KEY", "\"${localKey("PIXABAY_API_KEY")}\"")
    }

    buildTypes {
        release {
            // R8 shrinking makes Compose much faster than the debug build on the TV.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideload build signed with this Mac's local debug key (~/.android/debug.keystore).
            // A Play Store release would need its own upload key.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.work.runtime.ktx)

    testImplementation(libs.junit)
}
