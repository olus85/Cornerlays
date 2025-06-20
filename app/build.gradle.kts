plugins {
    id("com.android.application")
}

android {
    namespace = "app.olus.cornerlays"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.olus.cornerlays"
        minSdk = 23
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

    buildFeatures {
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // NEU: Verhindert Build-Fehler mit OkHttp/Kotlin-Metadaten
    packagingOptions {
        exclude("META-INF/kotlin-stdlib-common.kotlin_module")
        exclude("META-INF/kotlin-stdlib.kotlin_module")
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.android.volley:volley:1.2.1")

    // NEU: Für Home Assistant Integration
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}