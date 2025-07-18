plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.sharedpreferencesapp"
    compileSdk = 35  // ⬅️ CAMBIADO: De 34 a 35

    defaultConfig {
        applicationId = "com.example.sharedpreferencesapp"
        minSdk = 24
        targetSdk = 34  // ⬅️ MANTENER: targetSdk puede ser diferente
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.fragment)

    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-auth")

    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.itextpdf:itext7-core:7.2.5")
    // Otras dependencias
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.github.TutorialsAndroid:GButton:v1.0.19")


    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}