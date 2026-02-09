import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// 获取当前日期和版本信息
val buildDate = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
val buildTime = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())

// 版本信息
val versionCode = 3
val versionName = "1.0.0"

// 从 gradle.properties 获取车机平台类型
val carPlatformType = providers.gradleProperty("CAR_PLATFORM_TYPE").getOrElse("tinnove")
val includeInstrumentPanel = (providers.gradleProperty("INCLUDE_INSTRUMENT_PANEL").getOrElse("true")).toBoolean()
// 从 settings.gradle 获取 currentVehicleType 和各个车型类型值
val currentVehicleType: String = rootProject.ext["currentVehicleType"] as String
val tinnove_8678_d4: String? = rootProject.ext["tinnove_8678_d4"] as String
val tinnove_8155_d3: String? = rootProject.ext["tinnove_8155_d3"] as String
val mega_8155_d3: String? = rootProject.ext["mega_8155_d3"] as String
val mega_8155_g3: String? = rootProject.ext["mega_8155_g3"] as String
val mega_8295_d4: String? = rootProject.ext["mega_8295_d4"] as String
val enable_fragrance:String? = rootProject.ext["enable_fragrance"] as String

android {
    namespace = "com.mine.baselibrary"
    compileSdk = 36
    

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            buildConfigField("String", "CAR_PLATFORM_TYPE", "\"$carPlatformType\"")
            buildConfigField("boolean", "INCLUDE_INSTRUMENT_PANEL", includeInstrumentPanel.toString())
            buildConfigField("String", "CURRENT_VEHICLE_TYPE", "\"$currentVehicleType\"")
            buildConfigField("String", "tinnove_8678_d4", "\"${tinnove_8678_d4 ?: ""}\"")
            buildConfigField("String", "tinnove_8155_d3", "\"${tinnove_8155_d3 ?: ""}\"")
            buildConfigField("String", "mega_8155_d3", "\"${mega_8155_d3 ?: ""}\"")
            buildConfigField("String", "mega_8155_g3", "\"${mega_8155_g3 ?: ""}\"")
            buildConfigField("String", "mega_8295_d4", "\"${mega_8295_d4 ?: ""}\"")
            buildConfigField("String", "enable_fragrance", "\"${enable_fragrance ?: ""}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "CAR_PLATFORM_TYPE", "\"$carPlatformType\"")
            buildConfigField("boolean", "INCLUDE_INSTRUMENT_PANEL", includeInstrumentPanel.toString())
            buildConfigField("String", "CURRENT_VEHICLE_TYPE", "\"$currentVehicleType\"")
            buildConfigField("String", "tinnove_8678_d4", "\"${tinnove_8678_d4 ?: ""}\"")
            buildConfigField("String", "tinnove_8155_d3", "\"${tinnove_8155_d3 ?: ""}\"")
            buildConfigField("String", "mega_8155_d3", "\"${mega_8155_d3 ?: ""}\"")
            buildConfigField("String", "mega_8155_g3", "\"${mega_8155_g3 ?: ""}\"")
            buildConfigField("String", "mega_8295_d4", "\"${mega_8295_d4 ?: ""}\"")
            buildConfigField("String", "enable_fragrance", "\"${enable_fragrance ?: ""}\"")
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
        buildConfig = true
    }
}

// 使用 doLast 重命名 AAR 文件
val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

afterEvaluate {
    tasks.named("bundleDebugAar") {
        doLast {
            val outputDir = file("build/outputs/aar")
            val originalFile = file("$outputDir/baselibrary-debug.aar")
            val newFile = file("$outputDir/baselibrary-${date}-v${versionCode}-debug.aar")
            
            if (originalFile.exists()) {
                originalFile.renameTo(newFile)
                println("Renamed AAR file to: ${newFile.name}")
            }
        }
    }
    
    tasks.named("bundleReleaseAar") {
        doLast {
            val outputDir = file("build/outputs/aar")
            val originalFile = file("$outputDir/baselibrary-release.aar")
            val newFile = file("$outputDir/baselibrary-${date}-v${versionCode}-release.aar")
            
            if (originalFile.exists()) {
                originalFile.renameTo(newFile)
                println("Renamed AAR file to: ${newFile.name}")
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    
    // Activity相关依赖，用于权限请求
    implementation("androidx.activity:activity-ktx:1.8.2")

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit)
    // Timber logging
    implementation(libs.timber)
}