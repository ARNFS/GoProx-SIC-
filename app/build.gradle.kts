import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.goprox"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.goprox"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // API Keys from local.properties
        val groqApiKey = getLocalProperty("GROQ_API_KEY")
        val deepseekApiKey = getLocalProperty("DEEPSEEK_API_KEY")
        val agoraAppId = getLocalProperty("AGORA_APP_ID")  // ✅ MUST HAVE

        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"$deepseekApiKey\"")
        buildConfigField("String", "AGORA_APP_ID", "\"$agoraAppId\"")
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/versions/9/OSGI-INF/"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
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
}

fun getLocalProperty(key: String): String {
    val properties = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        properties.load(FileInputStream(localPropsFile))
    }
    return properties.getProperty(key, "")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Firebase BOM + products (արդեն ունես)
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-analytics")

    // Google Play Services
    implementation("com.google.android.gms:play-services-auth:21.5.1")

    // Google Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // JSON
    implementation("org.json:json:20251224")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // PhotoView
    implementation("com.github.chrisbanes:PhotoView:2.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // AndroidX & Material
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // ========== WEBRTC (ամենաթարմ 2026 թվականի տարբերակը) ==========
    implementation("io.getstream:stream-webrtc-android:1.3.10")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}