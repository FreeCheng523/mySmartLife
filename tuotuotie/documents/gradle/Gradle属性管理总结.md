# Gradle 属性管理总结

## 1. 从 `gradle.properties` 读取属性

在 `settings.gradle` 中，不能直接使用 `findProperty()`，需要手动读取文件：

```groovy
def getProperty = { String key ->
    def properties = new Properties()
    def propertiesFile = file("gradle.properties")
    if (propertiesFile.exists()) {
        propertiesFile.withInputStream { properties.load(it) }
        return properties.getProperty(key)
    }
    return null
}
```

## 2. 在 `settings.gradle` 中定义全局属性

**问题**：`settings.gradle` 中的 `ext` 和子项目中的 `rootProject.ext` 是不同的对象。

**解决方案**：使用 `gradle.projectsLoaded` 回调在项目加载后设置 `rootProject.ext`：

```groovy
// settings.gradle
gradle.projectsLoaded {
    gradle.rootProject.ext.vehicleType518 = vehicleType518Value
    gradle.rootProject.ext.currentVehicleType = vehicleTypeValue
}
```

## 3. 在子项目中直接读取 `gradle.properties` 中的属性

在子项目的 `build.gradle` 中，可以直接使用 `project.findProperty()` 或 `providers.gradleProperty()` 读取 `gradle.properties` 中的属性：

**Groovy DSL (`build.gradle`)**：
```groovy
// 方式1：使用 findProperty（推荐）
def carPlatformType = project.findProperty("CAR_PLATFORM_TYPE") ?: "none"
def currentVehicleType = project.findProperty("CURRENT_VEHICLE_TYPE") ?: "none"
def includeInstrumentPanel = project.findProperty("INCLUDE_INSTRUMENT_PANEL") ?: "false"
```

**Kotlin DSL (`build.gradle.kts`)**：
```kotlin
// 方式1：使用 providers.gradleProperty（推荐）
val carPlatformType = providers.gradleProperty("CAR_PLATFORM_TYPE").getOrElse("none")
val currentVehicleType = providers.gradleProperty("CURRENT_VEHICLE_TYPE").getOrElse("none")
val includeInstrumentPanel = (providers.gradleProperty("INCLUDE_INSTRUMENT_PANEL").getOrElse("true")).toBoolean()

// 方式2：使用 findProperty
val carPlatformType2 = project.findProperty("CAR_PLATFORM_TYPE") as String? ?: "none"
```

**注意事项**：
- `project.findProperty()` 返回 `Object?`，需要类型转换
- `providers.gradleProperty()` 返回 `Provider<String>`，需要使用 `getOrElse()` 获取值
- 如果属性不存在，`findProperty()` 返回 `null`，`providers.gradleProperty()` 需要提供默认值

## 4. 在子项目中访问 `rootProject.ext`

**Groovy DSL (`build.gradle`)**：
```groovy
def currentVehicleType = rootProject.ext.has("currentVehicleType") 
    ? rootProject.ext.currentVehicleType 
    : "none"
```

**Kotlin DSL (`build.gradle.kts`)**：
```kotlin
val currentVehicleType: String = rootProject.ext["currentVehicleType"] as String
```

## 5. 属性访问时机问题

- `settings.gradle` 中的 `ext` 是 Settings 对象的扩展属性
- `rootProject.ext` 是 Project 对象的扩展属性
- 在 `include` 之前，`rootProject` 可能尚未完全初始化
- 使用 `gradle.projectsLoaded` 回调确保在正确的时机设置属性

## 6. 安全检查

在子项目中访问 `rootProject.ext` 时，添加存在性检查：

```groovy
// 检查属性是否存在
if (rootProject.ext.has("propertyName")) {
    def value = rootProject.ext.propertyName
}
```

## 7. 将属性传递到代码中

通过 `buildConfigField` 将 Gradle 属性传递到 `BuildConfig`：

```kotlin
// build.gradle.kts
buildConfigField("String", "CURRENT_VEHICLE_TYPE", "\"$currentVehicleType\"")
```

然后在代码中访问：
```kotlin
// Kotlin
const val CURRENT_VEHICLE_TYPE = BuildConfig.CURRENT_VEHICLE_TYPE
```

## 8. 关键要点

1. `settings.gradle` 的 `ext` 不会自动复制到 `rootProject.ext`
2. 使用 `gradle.projectsLoaded` 回调手动设置 `rootProject.ext`
3. 在 `settings.gradle` 中读取 `gradle.properties` 需要手动解析文件
4. 在子项目的 `build.gradle` 中可以直接使用 `project.findProperty()` 或 `providers.gradleProperty()` 读取 `gradle.properties`
5. 子项目访问 `rootProject.ext` 时添加安全检查，避免属性不存在导致的错误
6. Kotlin DSL 使用方括号语法：`rootProject.ext["key"]`
7. Groovy DSL 使用点号语法：`rootProject.ext.key`

这些方法可以确保在 Gradle 多项目构建中正确管理和共享属性。

