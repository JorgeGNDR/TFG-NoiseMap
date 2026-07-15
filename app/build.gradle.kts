import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.1.10-1.0.29"
}

fun firebaseConfigValues(): Map<String, String> {
    val googleServicesFile = file("google-services.json")
    if (!googleServicesFile.exists()) return emptyMap()

    val json = JsonSlurper().parse(googleServicesFile) as Map<*, *>
    val projectInfo = json["project_info"] as Map<*, *>
    val clients = json["client"] as List<*>
    val client = clients
        .map { it as Map<*, *> }
        .firstOrNull {
            val clientInfo = it["client_info"] as? Map<*, *>
            val androidClientInfo = clientInfo?.get("android_client_info") as? Map<*, *>
            androidClientInfo?.get("package_name") == "com.gandara.tfgjorgegandara"
        } ?: clients.first() as Map<*, *>

    val clientInfo = client["client_info"] as Map<*, *>
    val apiKey = (client["api_key"] as List<*>).first() as Map<*, *>

    return mapOf(
        "google_app_id" to clientInfo["mobilesdk_app_id"].toString(),
        "google_api_key" to apiKey["current_key"].toString(),
        "project_id" to projectInfo["project_id"].toString(),
        "gcm_defaultSenderId" to projectInfo["project_number"].toString(),
        "google_storage_bucket" to projectInfo["storage_bucket"].toString()
    )
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}

fun buildConfigString(value: String): String {
    val escaped = value.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"$escaped\""
}

android {
    namespace = "com.gandara.tfgjorgegandara"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.gandara.tfgjorgegandara"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "MAPTILER_API_KEY",
            buildConfigString(localProperties.getProperty("MAPTILER_API_KEY", ""))
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        firebaseConfigValues().forEach { (name, value) ->
            resValue("string", name, value)
        }
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
        buildConfig = true
        compose = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room (Base de datos)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // Uso de KSP para generar código de compilación

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // MapLibre (Mapas vectoriales y heatmap profesional)
    implementation("org.maplibre.gl:android-sdk:13.1.0")

    // Firebase AI Logic (Gemini Developer API)
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-ai")

    // YAMNNET (TFLite)
    implementation(libs.tensorflow.lite)
    implementation("org.tensorflow:tensorflow-lite-task-audio:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // JTransforms (Matemáticas y FFT)
    implementation("com.github.wendykierp:JTransforms:3.1")

    // Navegación Compose y ViewModel
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
