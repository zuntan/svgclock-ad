plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.zuntan.svgclock_ad"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.zuntan.svgclock_ad"
        minSdk = 30
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.fragment)
    // https://mvnrepository.com/artifact/kxml2/kxml2
    implementation("kxml2:kxml2:2.3.0")
    implementation("androidx.ink:ink-geometry:1.0.0-alpha06")

    implementation("com.akuleshov7:ktoml-core:0.7.0")
    implementation("com.akuleshov7:ktoml-file:0.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.1")
    implementation("com.caverock:androidsvg-aar:1.4")

    testImplementation(libs.junit)
    testImplementation("kxml2:kxml2:2.3.0")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}