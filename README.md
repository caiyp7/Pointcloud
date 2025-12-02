# PointCloud

一个基于 Android 的点云处理和可视化应用，支持 UDP 数据接收和 WebSocket 数据传输。

## 项目简介

该项目是一个 Android 应用程序，用于处理点云数据，支持通过 UDP 接收数据并通过 WebSocket 转发，包含 C++ 原生库用于高性能数据处理。

## 系统要求

- **操作系统**: Android 9.0 (API 28) 及以上
- **开发环境**: 
  - Android Studio Hedgehog (2023.1.1) 或更高版本
  - Gradle 8.12.1
  - JDK 11 或更高版本
  - CMake 3.22.1 或更高版本
  - NDK (支持 arm64-v8a 架构)

## 依赖项

项目使用以下主要依赖：

- Kotlin 2.0.21
- AndroidX Core KTX 1.10.1
- Material Components 1.12.0
- ConstraintLayout 2.2.1
- Java-WebSocket 1.5.6

详细依赖配置请查看 `gradle/libs.versions.toml` 文件。

## 快速开始

### 1. 克隆项目

```bash
git clone <your-repository-url>
cd PointCloud
```

### 2. 配置 Android SDK

项目会自动检测您的 Android SDK 位置。如果需要手动配置，请在项目根目录创建 `local.properties` 文件：

```properties
sdk.dir=/path/to/your/Android/Sdk
```

**注意**: `local.properties` 文件已在 `.gitignore` 中，不会被提交到版本控制系统。

### 3. 打开项目

1. 启动 Android Studio
2. 选择 "Open an existing project"
3. 选择项目根目录
4. 等待 Gradle 同步完成

### 4. 构建项目

#### 使用 Android Studio

- 点击菜单栏的 `Build` -> `Make Project` 或按 `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)

#### 使用命令行

```bash
# Linux/Mac
./gradlew build

# Windows
gradlew.bat build
```

### 5. 运行应用

1. 连接 Android 设备或启动模拟器（需要支持 arm64-v8a 架构）
2. 点击 Android Studio 工具栏的运行按钮或按 `Shift+F10`

## 项目结构

```
PointCloud/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── cpp/              # C++ 原生代码
│   │   │   │   ├── CMakeLists.txt
│   │   │   │   ├── ground/       # 地面处理相关代码
│   │   │   │   ├── jni/          # JNI 接口
│   │   │   │   ├── third_party/  # 第三方库
│   │   │   │   └── native-lib.cpp
│   │   │   ├── java/             # Kotlin 源代码
│   │   │   │   └── com/example/pointcloud/
│   │   │   │       └── net/
│   │   │   │           └── UdpToWebSocketService.kt
│   │   │   ├── res/              # 资源文件
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/          # Android 测试
│   │   └── test/                 # 单元测试
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   ├── libs.versions.toml        # 依赖版本管理
│   └── wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## 主要功能

- UDP 数据接收
- WebSocket 数据转发
- 原生 C++ 点云处理
- 支持 arm64-v8a 架构

## 配置说明

### NDK 配置

项目配置为仅支持 `arm64-v8a` 架构。如需支持其他架构，请修改 `app/build.gradle.kts` 中的 `ndk.abiFilters` 设置：

```kotlin
ndk {
    abiFilters += setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
}
```

### CMake 版本

项目使用 CMake 3.22.1。如果需要更改版本，请修改 `app/build.gradle.kts` 中的 `externalNativeBuild.cmake.version` 配置。

## 常见问题

### 1. Gradle 同步失败

- 检查网络连接
- 确保已安装正确版本的 JDK
- 尝试删除 `.gradle` 目录后重新同步

### 2. NDK 构建错误

- 确保已通过 SDK Manager 安装 NDK
- 检查 CMake 版本是否正确
- 验证 C++ 代码的编译选项

### 3. 设备不兼容

- 确保设备运行 Android 9.0 或更高版本
- 检查设备是否支持 arm64-v8a 架构

## 开发指南

### 添加新的依赖

在 `gradle/libs.versions.toml` 文件中添加依赖版本和库定义，然后在 `app/build.gradle.kts` 中引用。

### 修改 C++ 代码

C++ 代码位于 `app/src/main/cpp/` 目录。修改后需要重新构建项目。

## 贡献

欢迎提交 Issue 和 Pull Request。

## 许可证

[添加您的许可证信息]

## 联系方式

[添加您的联系方式]

