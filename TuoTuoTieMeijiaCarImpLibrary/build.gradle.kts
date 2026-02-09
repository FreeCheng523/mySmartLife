plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("dagger.hilt.android.plugin") // 应用 Hilt 插件
    id("kotlin-kapt") // 启用 KAPT（Kotlin 注解处理器）
}

android {
    namespace = "com.smartlife.tuotuotiemeijiacarimplibrary"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        buildConfig = true
    }
}

dependencies {
    api(project(":tuotuotie_car_interface_library"))
    
    // 添加Hilt依赖
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    // 使用 flatDir 仓库引用本地文件（Kotlin DSL 语法）
    compileOnly(group = "", name = "bigsur-car-lib-release", ext = "aar")
    compileOnly(group = "", name = "carlib-new-1.0.2.918-175cb6c", ext = "aar")
    compileOnly(group = "", name = "MLog-1.0.1.13-78dbef5", ext = "aar")
    compileOnly(group = "", name = "nexus-static-1.236.3.0-4e910b2", ext = "jar")
    compileOnly(group = "", name = "smartlifemiddleware-release", ext = "aar")
    compileOnly(group = "", name = "android.car8678", ext = "jar")
    implementation(project(":baselibrary"))
    api(libs.timber)
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}