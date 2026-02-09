plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("dagger.hilt.android.plugin") // 应用 Hilt 插件
    id("kotlin-kapt") // 启用 KAPT（Kotlin 注解处理器）
}

android {
    namespace = "com.smarlife.tuotiecarimpllibrary"
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
    implementation(project(":tuotuotie_car_interface_library"))
    
    // 添加Hilt依赖
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(project(":baselibrary"))
    api(libs.timber)
    
    // 使用 flatDir 仓库引用本地文件（Kotlin DSL 语法）
    compileOnly(group = "", name = "offline_C518-1.0.0.d_release", ext = "jar")
    compileOnly(group = "", name = "android.car8678", ext = "jar")
    compileOnly(group = "", name = "virtualcar-platform-1.0.7.i", ext = "aar")
    


}