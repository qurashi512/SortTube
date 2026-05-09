plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.grieztech.ytorganizer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.grieztech.ytorganizer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        resourceConfigurations += setOf("ar", "en")  // ← هنا

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "YOUTUBE_API_KEY", "\"AIzaSyChD8wDh9E3tITRiNnQ0mqkbNZGJBZQxsE\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"647341706002-injbuck9qp9i99vjab06i1pv68m5cjlk.apps.googleusercontent.com\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    // ── Core AndroidX ──────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ── Jetpack Compose (BOM) ──────────────────────────────────
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Navigation ─────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // ── ViewModel ──────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // ── DataStore ──────────────────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.2.1")

    // ── Retrofit ───────────────────────────────────────────────
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ── Google Sign-In ─────────────────────────────────────────
    implementation("com.google.android.gms:play-services-auth:21.5.0")

    // ── Coil (صور) ─────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Reorderable List ───────────────────────────────────────
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

    // ── Hilt (Dependency Injection) ────────────────────────────
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ── Room (قاعدة البيانات) ──────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── Splash Screen ──────────────────────────────────────────
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ── Tests ──────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-config")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")
}
