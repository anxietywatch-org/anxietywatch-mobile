plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services) apply false
}

// CI no almacena google-services.json. Firebase se configura en builds que sí tienen el archivo local.
if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.anxietywatch.mobile"
    compileSdk = 36 // el propio wizard ya seleccionó 36 (Android Studio muy reciente) — lo respetamos

    defaultConfig {
        applicationId = "com.anxietywatch.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField(
            "boolean",
            "ENABLE_VERBOSE_NETWORK_LOGGING",
            (providers.gradleProperty("enableVerboseNetworkLogging").orNull?.toBoolean() ?: false).toString(),
        )
    }

    buildTypes {
        release {
            // DevSecOps: ofusca y elimina codigo/recursos sin usar en la build de release.
            // Dificulta ingenieria inversa y reduce superficie de ataque (menos codigo
            // legible = menos pistas si alguien descompila el APK).
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true // necesario para BuildConfig.DEBUG (ver ApiClient.kt)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // --- Compose + Material3 (mismas versiones que ya trae tu libs.versions.toml) ---
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // --- Iconos reales del diseño Stitch (self_improvement, history_edu, bedtime, air, spa...) ---
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

    // --- DevSecOps: cifrado del JWT en disco, respaldado por Android Keystore (hardware) ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    debugImplementation(libs.androidx.compose.ui.tooling)

    // --- Navigation ---
    implementation(libs.androidx.navigation.compose)

    // --- Dependency injection ---
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // --- Red: hablar con AnxietyWatchAPI (.NET) ---
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // --- Sesión / token guardado ---
    implementation(libs.androidx.datastore.preferences)

    // --- Cache local offline-first ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // --- Sync de respaldo cuando no hay red ---
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // --- EL PUENTE CON EL RELOJ ---
    implementation(libs.play.services.wearable)

    // --- Ubicación (payload de SOS) ---
    implementation(libs.play.services.location)

    // --- Tests (los que ya traía la plantilla) ---
    testImplementation(libs.junit)
    testImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
