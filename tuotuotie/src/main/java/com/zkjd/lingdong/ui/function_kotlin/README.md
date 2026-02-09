# Function Kotlin 模块

这是一个使用传统Android XML布局实现的功能配置模块，与原有的Compose版本功能完全一致。

## 模块结构

```
function_kotlin/
├── FunctionActivity.kt              # 主功能配置Activity
├── FunctionFragment.kt              # 功能配置Fragment
├── FunctionViewModelKotlin.kt       # 功能配置ViewModel
├── AppSelectionActivity.kt          # 应用选择Activity
├── AppSelectionViewModelKotlin.kt   # 应用选择ViewModel
├── ButtonTypeAdapter.kt             # 按钮类型适配器
├── FunctionGridAdapter.kt           # 功能网格适配器
├── AppGridAdapter.kt                # 应用网格适配器
└── README.md                        # 本说明文件
```

## XML布局文件

```
res/layout/
├── activity_function.xml            # 主功能配置Activity布局
├── fragment_function.xml            # 功能配置Fragment布局
├── activity_app_selection.xml       # 应用选择Activity布局
├── item_button_type.xml             # 按钮类型列表项布局
├── item_function_grid.xml           # 功能网格项布局
└── item_app_grid.xml                # 应用网格项布局
```

## 主要功能

### 1. FunctionActivity
- 作为容器Activity，只负责Toolbar和Fragment管理
- 处理与Fragment的通信（保存操作、未保存更改提醒等）
- 支持返回键处理和保存确认对话框

### 2. FunctionFragment
- 主要的功能配置界面，包含所有业务逻辑
- 支持多种按键类型配置（短按、双击、旋转等）
- 集成防误触开关控制
- 支持临时保存和批量保存功能配置
- 可嵌入到其他Activity中使用

### 3. AppSelectionActivity
- 应用选择界面
- 支持搜索已安装应用
- 网格布局展示应用列表

### 4. RecyclerView适配器
- **ButtonTypeAdapter**: 按钮类型选择适配器
- **FunctionGridAdapter**: 功能网格显示适配器
- **AppGridAdapter**: 应用列表适配器

## 使用方式

### 启动功能配置Activity（推荐）
```kotlin
val intent = Intent(context, FunctionActivity::class.java)
intent.putExtra(FunctionActivity.EXTRA_DEVICE_MAC, deviceMac)
startActivity(intent)
```
Activity会自动创建和管理FunctionFragment，并处理Toolbar操作。

### 直接使用Fragment（高级用法）
```kotlin
val fragment = FunctionFragment.newInstance(deviceMac)
supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .commit()
```
需要手动实现FunctionFragment.FunctionFragmentListener接口来处理Fragment通信。

## 与Compose版本的对比

| 特性 | Compose版本 | XML版本 |
|------|-------------|---------|
| UI框架 | Jetpack Compose | 传统XML + RecyclerView |
| 状态管理 | StateFlow + collectAsState | StateFlow + lifecycleScope |
| 导航 | Navigation Compose | Intent + Activity |
| 布局方式 | 声明式UI | 命令式UI |
| 性能 | 现代化渲染 | 传统View系统 |
| 学习曲线 | 需要Compose知识 | 传统Android开发 |

## 技术特点

1. **MVVM架构**: 使用ViewModel管理业务逻辑
2. **响应式编程**: 基于StateFlow的状态管理
3. **依赖注入**: 使用Hilt进行依赖注入
4. **Activity+Fragment架构**: Activity作为容器，Fragment承载业务逻辑
5. **模块化设计**: 清晰的职责分离，Fragment可复用
6. **用户体验**: 支持搜索、筛选、临时保存等功能

## 架构优势

### Activity+Fragment分离的好处：
- **职责清晰**: Activity只负责容器管理，Fragment专注业务逻辑
- **复用性强**: Fragment可以嵌入到不同的Activity中
- **维护性好**: 业务逻辑和UI容器分离，便于测试和维护
- **扩展性强**: 可以轻松添加多Fragment支持或导航逻辑

## 注意事项

1. 确保在AndroidManifest.xml中注册Activity
2. 需要相应的权限来查询已安装应用
3. 图标资源需要在drawable目录中提供
4. 颜色资源在colors_function.xml中定义

## 扩展性

该模块设计具有良好的扩展性：
- 可以轻松添加新的功能类型
- 适配器支持动态数据更新
- ViewModel可以扩展更多业务逻辑
- 布局可以根据需要调整样式
