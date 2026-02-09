import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("dagger.hilt.android.plugin") // 应用 Hilt 插件
    id("kotlin-kapt") // 启用 KAPT（Kotlin 注解处理器）
}

android {
    namespace = "com.zkjd.lingdong"
    compileSdk = 34

    defaultConfig {
        //applicationId = "com.zkjd.lingdong"
        minSdk = 28
        targetSdk = 34
        //versionCode = 1
        //versionName = "1.0.1"

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
        languageVersion = "1.9"
        apiVersion = "1.9"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    lint {
        disable += "MissingPermission"
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {

    implementation(project(":tuotuotie_car_interface_library"))
    implementation(project(":fragrance"))

    // 从 settings.gradle 获取 currentVehicleType
    val currentVehicleType: String = rootProject.ext["currentVehicleType"] as String



    when(currentVehicleType){
        "tinnove_8678_d4","tinnove_8155_d3" ->{
            implementation(project(":TuoTuoTieTinnoveCarImplLibrary"))
        }
        "mega_8155_g3","mega_8295_d4","mega_8155_d3"->{
            implementation(project(":TuoTuoTieMeijiaCarImpLibrary"))
        }

    }
    
    val roomVersion = "2.5.2"


    // 或者直接引用特定的jar文件
    //implementation(files("libs/android.car.jar"))
    //implementation(files("libs/virtualcar-platform-1.0.7.i.aar"))
    //implementation(files("libs/offline-platform-C857-1.0.1.a.aar"))

    implementation(project(":baselibrary"))

/*    compileOnly(files("../libs/mega_sdk_v3.1_20250416/bigsur-car-lib-release.aar"))
    compileOnly(files("../libs/mega_sdk_v3.1_20250416/carlib-new-1.0.2.918-175cb6c.aar"))
    compileOnly(files("../libs/mega_sdk_v3.1_20250416/MLog-1.0.1.13-78dbef5.aar"))
    compileOnly(files("../libs/mega_sdk_v3.1_20250416/nexus-static-1.236.3.0-4e910b2.jar"))

    compileOnly(files("../libs/tinnove/offline_C518-1.0.0.d_release.jar"))
    compileOnly(files("../libs/tinnove/virtualcar-platform-1.0.7.i.aar"))*/
    /*compileOnly(files("../libs/tinnove/android.car8678.jar"))*/

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Gson
    implementation(libs.gson)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)
    
    // Navigation
//    implementation(libs.androidx.navigation.compose)

//    // Car
//    //implementation(libs.androidx.app)
//    implementation(libs.androidx.car)
//    implementation(libs.car)
//    implementation(libs.androidx.car.car)

    // Timber
    api(libs.timber)
    
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    


    implementation(libs.appcompat)
    implementation(libs.material)

//    implementation (libs.android.wutong)
//    implementation (libs.android.wutongsys)
//
//    implementation(libs.android.app.car) // 依据实际需求选择合适的版本
//    implementation(libs.androidx.car)
    testImplementation(kotlin("test"))
}