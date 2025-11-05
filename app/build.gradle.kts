import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.firebase.appdistribution)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.ysdigi.puratrip"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ysdigi.puratrip"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "2-LTE"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

val versionPropsFile = file("version.properties")
val versionProps = Properties()
versionProps.load(versionPropsFile.inputStream())

val versionCode = versionProps["versionCode"].toString().toInt()
val versionName = versionProps["versionName"].toString()

android.defaultConfig.versionCode = versionCode
android.defaultConfig.versionName = versionName

dependencies {

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.gif)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.register("incrementVersion") {
    doLast {
        val newVersionCode = versionCode + 1
        val newVersionName = versionName.split(".").let {
            val major = it[0].toInt()
            val minor = it[1].toInt() + 1
            "$major.$minor"
        }
        versionProps["versionCode"] = newVersionCode.toString()
        versionProps["versionName"] = newVersionName
        versionProps.store(versionPropsFile.writer(), null)
    }
}

tasks.named("preBuild") {
    dependsOn("incrementVersion")
}
