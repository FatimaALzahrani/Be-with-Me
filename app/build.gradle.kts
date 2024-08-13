plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
//    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.waqf.bewithme"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.waqf.bewithme"
        minSdk = 27
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-identity-license:12.0.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.google.firebase:firebase-auth:23.0.0")
    implementation("com.google.firebase:firebase-database:21.0.0")

//    implementation("com.github.AgoraIO-Community:Android-UIKit:v2.0.0")
//    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
//    implementation( "androidx.navigation:navigation-compose:2.8.0-beta07")
//    implementation ("androidx.activity:activity-compose:1.9.1")
//    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
//    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
//    implementation ("io.agora.rtc:full-sdk:4.0.1")
    implementation("io.agora.rtc:full-rtc-basic:4.4.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}