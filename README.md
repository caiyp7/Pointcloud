# PointCloud

一个基于 Android 的点云处理和可视化应用，支持 UDP 数据接收和 WebSocket 数据传输。

## 📋 快速部署检查清单

在开始之前，确保完成以下准备：

- [ ] ✅ 已安装 JDK 11 或更高版本（`java -version` 检查）
- [ ] ✅ 已安装 Android Studio Hedgehog 2023.1.1 或更高版本
- [ ] ✅ 通过 SDK Manager 安装了：NDK、CMake 3.22.1+、Android SDK Build-Tools
- [ ] ✅ 有稳定的网络连接（首次构建需要下载依赖）
- [ ] ✅ 至少 10GB 可用磁盘空间
- [ ] ✅ 有支持 arm64-v8a 的 Android 设备或模拟器（Android 9.0+）

## 🚀 三步快速开始

```bash
# 1. 克隆项目（必须使用 --recursive）
git clone --recursive https://github.com/caiyp7/Pointcloud.git

# 2. 用 Android Studio 打开项目
# File -> Open -> 选择 Pointcloud 目录

# 3. 点击运行按钮（▶️）
```

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

### 1. 安装开发工具

#### 1.1 安装 JDK 11 或更高版本

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install openjdk-11-jdk
java -version  # 验证安装
```

**Windows:**
- 下载 [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) 或 [OpenJDK](https://adoptium.net/)
- 运行安装程序
- 配置 `JAVA_HOME` 环境变量

**macOS:**
```bash
brew install openjdk@11
```

#### 1.2 安装 Android Studio

1. **下载 Android Studio**
   - 访问 [Android Studio 官网](https://developer.android.com/studio)
   - 下载最新版本（Hedgehog 2023.1.1 或更高版本）

2. **安装 Android Studio**
   - **Linux**: 解压后运行 `bin/studio.sh`
   - **Windows**: 运行安装程序 `.exe`
   - **macOS**: 打开 `.dmg` 文件并拖拽到 Applications 文件夹

3. **首次启动配置**
   - 启动 Android Studio
   - 按照向导完成初始配置
   - 选择 "Standard" 安装类型（会自动安装 Android SDK）

#### 1.3 安装必需的 SDK 组件

1. 打开 Android Studio
2. 点击 `Tools` -> `SDK Manager`（或欢迎界面的 `More Actions` -> `SDK Manager`）
3. 在 **SDK Platforms** 标签页:
   - ✅ 勾选 `Android 14.0 (API 35)`（或项目使用的版本）
4. 在 **SDK Tools** 标签页，勾选以下工具:
   - ✅ `Android SDK Build-Tools`
   - ✅ `NDK (Side by side)` - 选择最新版本
   - ✅ `CMake` - 选择 3.22.1 或更高版本
   - ✅ `Android SDK Command-line Tools`
   - ✅ `Android Emulator`（如果需要使用模拟器）
5. 点击 `Apply` 开始下载和安装
6. 等待安装完成（可能需要几分钟到十几分钟）

### 2. 克隆项目

**⚠️ 重要：必须使用 `--recursive` 参数克隆，以下载 draco 第三方库！**

```bash
# 使用 SSH（推荐，需要配置 SSH 密钥）
git clone --recursive git@github.com:caiyp7/Pointcloud.git
cd Pointcloud

# 或使用 HTTPS
git clone --recursive https://github.com/caiyp7/Pointcloud.git
cd Pointcloud
```

**如果忘记使用 `--recursive` 参数：**

```bash
# 补救方法：手动初始化 submodules
git submodule update --init --recursive
```

### 3. 打开并配置项目

#### 3.1 用 Android Studio 打开项目

1. 启动 Android Studio
2. 选择 `File` -> `Open` (或欢迎界面的 `Open`)
3. 导航到克隆的项目目录，选择根目录（包含 `build.gradle.kts` 的目录）
4. 点击 `OK`

#### 3.2 等待 Gradle 同步

- Android Studio 会自动开始同步 Gradle
- 首次同步会下载依赖，可能需要 5-15 分钟（取决于网络速度）
- 同步过程会显示在底部状态栏
- **如果遇到网络问题**，可以配置镜像源（见下方"常见问题"）

#### 3.3 验证配置

项目会自动检测 Android SDK 位置并创建 `local.properties` 文件。如需手动配置：

```properties
# local.properties（项目根目录，如不存在则创建）
sdk.dir=/path/to/your/Android/Sdk

# Linux 默认路径示例
sdk.dir=/home/username/Android/Sdk

# Windows 默认路径示例
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# macOS 默认路径示例
sdk.dir=/Users/username/Library/Android/sdk
```

**注意**: `local.properties` 文件已在 `.gitignore` 中，不会被提交。

### 4. 构建项目

#### 4.1 使用 Android Studio（推荐）

1. 等待 Gradle 同步完成
2. 点击菜单栏 `Build` -> `Make Project`
3. 或使用快捷键：
   - Windows/Linux: `Ctrl+F9`
   - macOS: `Cmd+F9`
4. 查看 `Build` 窗口的输出，确保构建成功

#### 4.2 使用命令行

```bash
# Linux/Mac
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug

# 完整构建（包括测试）
./gradlew build
```

构建成功后，APK 文件位于：`app/build/outputs/apk/debug/app-debug.apk`

### 5. 运行应用

#### 5.1 准备运行环境

**选项 A: 使用真实设备（推荐）**

1. 在 Android 设备上启用开发者选项：
   - 进入 `设置` -> `关于手机`
   - 连续点击 `版本号` 7 次
2. 启用 USB 调试：
   - 进入 `设置` -> `开发者选项`
   - 打开 `USB 调试`
3. 用 USB 连接设备到电脑
4. 设备上弹出提示时，选择 `允许 USB 调试`

**选项 B: 使用模拟器**

1. 点击 Android Studio 工具栏的 `Device Manager`
2. 点击 `Create Device`
3. 选择设备型号（推荐 Pixel 系列）
4. 选择系统镜像（必须是 API 28 或更高，且支持 arm64-v8a）
   - ⚠️ 注意：本项目仅支持 `arm64-v8a`，请选择相应架构的模拟器
5. 完成创建并启动模拟器

#### 5.2 运行应用

1. 在 Android Studio 工具栏选择目标设备
2. 点击绿色的 `Run` 按钮（▶️）
3. 或使用快捷键 `Shift+F10`（Windows/Linux）/ `Ctrl+R`（macOS）
4. 应用会自动安装并启动

### 6. 验证部署成功

应用成功启动后，你应该看到：
- 应用主界面显示
- 可以正常使用 UDP 和 WebSocket 功能
- 没有崩溃或错误提示

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

### 1. Gradle 同步失败或下载缓慢

**问题**: 首次打开项目时 Gradle 同步很慢或失败

**解决方案 A: 配置国内镜像（中国用户推荐）**

编辑项目根目录的 `build.gradle.kts`，将仓库配置改为使用阿里云镜像：

```kotlin
// 在 pluginManagement 和 dependencyResolutionManagement 块中的 repositories 改为：
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    google()
    mavenCentral()
}
```

**解决方案 B: 清除缓存重试**

```bash
# 删除本地 Gradle 缓存
rm -rf ~/.gradle/caches/

# 或在项目目录
rm -rf .gradle/

# 然后在 Android Studio 中: File -> Invalidate Caches and Restart
```

**解决方案 C: 检查 JDK 版本**

```bash
java -version  # 应该显示 11 或更高版本
```

### 2. NDK 构建错误

**错误信息**: `No version of NDK matched the requested version` 或 `CMake not found`

**解决方案**:

1. 打开 `Tools` -> `SDK Manager` -> `SDK Tools` 标签页
2. 确保已勾选并安装：
   - ✅ NDK (Side by side)
   - ✅ CMake 3.22.1+
3. 如果已安装但仍报错，检查 `local.properties`：

```properties
# 添加以下行（路径根据实际情况调整）
ndk.dir=/home/username/Android/Sdk/ndk/26.1.10909125
cmake.dir=/home/username/Android/Sdk/cmake/3.22.1
```

### 3. Submodule (draco) 未下载

**问题**: 编译时提示找不到 draco 相关头文件

**解决方案**:

```bash
# 初始化并更新 submodules
git submodule update --init --recursive

# 如果仍然有问题，强制重新克隆
git submodule foreach --recursive git reset --hard
git submodule update --force --recursive
```

### 4. 设备/模拟器不兼容

**错误**: `INSTALL_FAILED_NO_MATCHING_ABIS`

**原因**: 项目仅支持 `arm64-v8a` 架构

**解决方案 A: 使用支持的真实设备**
- 确保设备是 64 位架构
- Android 9.0 (API 28) 或更高版本

**解决方案 B: 创建正确架构的模拟器**
- 在创建模拟器时选择 `arm64-v8a` 系统镜像
- 或选择 `x86_64` 并修改项目支持（见下方）

**解决方案 C: 修改项目支持多架构**

编辑 `app/build.gradle.kts`：

```kotlin
ndk {
    // 添加更多架构支持
    abiFilters += setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
}
```

**注意**: 添加更多架构会增加 APK 大小。

### 5. JDK 版本不匹配

**错误**: `Unsupported class file major version` 或 `Android Gradle plugin requires Java 11`

**解决方案**:

1. 在 Android Studio 中配置 JDK:
   - `File` -> `Project Structure` -> `SDK Location`
   - 设置 `JDK location` 为 JDK 11 或更高版本

2. 或通过环境变量:

```bash
# Linux/Mac - 添加到 ~/.bashrc 或 ~/.zshrc
export JAVA_HOME=/path/to/jdk-11
export PATH=$JAVA_HOME/bin:$PATH

# 重新加载
source ~/.bashrc
```

### 6. 编译时内存不足

**错误**: `OutOfMemoryError: Java heap space`

**解决方案**: 编辑 `gradle.properties`，增加内存限制：

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### 7. Git clone 速度很慢

**问题**: 克隆项目或 submodules 很慢（特别是 draco）

**解决方案 A: 使用国内 Git 镜像**（仅适用于某些公共项目）

**解决方案 B: 浅克隆**

```bash
# 只克隆最新提交，减少下载量
git clone --recursive --depth 1 https://github.com/caiyp7/Pointcloud.git
```

**解决方案 C: 单独处理 submodule**

```bash
# 先克隆主项目
git clone https://github.com/caiyp7/Pointcloud.git
cd Pointcloud

# 后台下载 submodule
git submodule update --init --recursive --depth 1
```

### 8. USB 调试无法连接

**问题**: Android Studio 检测不到设备

**解决方案**:

1. **检查 USB 调试是否启用**（设备上）
2. **更换 USB 线**：使用数据线而非仅充电线
3. **安装 ADB 驱动**（Windows）：
   - 下载对应设备的 USB 驱动
   - 或使用 [Universal ADB Driver](https://adb.clockworkmod.com/)
4. **重启 ADB 服务**：

```bash
adb kill-server
adb start-server
adb devices  # 应该列出你的设备
```

5. **检查 USB 模式**：在设备上将 USB 模式改为 "文件传输" 或 "MTP"

### 9. 构建成功但应用闪退

**排查步骤**:

1. **查看 Logcat**：
   - Android Studio 底部 `Logcat` 标签页
   - 过滤 `Error` 或搜索应用包名 `com.example.pointcloud`

2. **检查权限**：
   - 确保在设备上授予了必要的权限（网络权限等）

3. **检查设备架构**：
   - 确认设备支持 arm64-v8a

```bash
adb shell getprop ro.product.cpu.abi
# 输出应包含 arm64-v8a
```

### 10. CMake 编译 C++ 代码失败

**错误**: CMake 找不到某些头文件或库

**解决方案**:

1. **清理构建缓存**：
   - `Build` -> `Clean Project`
   - `Build` -> `Rebuild Project`

2. **删除 C++ 构建缓存**：

```bash
rm -rf app/.cxx/
rm -rf app/build/
```

3. **验证 CMake 版本**：
   - 打开 `app/build.gradle.kts`
   - 检查 `externalNativeBuild.cmake.version` 是否匹配已安装版本

## 开发指南

### 添加新的依赖

在 `gradle/libs.versions.toml` 文件中添加依赖版本和库定义，然后在 `app/build.gradle.kts` 中引用。

### 修改 C++ 代码

C++ 代码位于 `app/src/main/cpp/` 目录。修改后需要重新构建项目。

## 🔧 开发工具下载链接

- [Android Studio](https://developer.android.com/studio) - 官方 IDE
- [JDK 下载](https://adoptium.net/) - OpenJDK（推荐）
- [Git 下载](https://git-scm.com/downloads) - 版本控制工具
- [Android 开发者文档](https://developer.android.com/docs) - 官方文档

## 📖 相关技术文档

- [Kotlin 官方文档](https://kotlinlang.org/docs/home.html)
- [Android NDK 指南](https://developer.android.com/ndk/guides)
- [CMake 文档](https://cmake.org/documentation/)
- [Draco 3D 压缩库](https://github.com/google/draco)
- [Java-WebSocket 库](https://github.com/TooTallNate/Java-WebSocket)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

### 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📝 更新日志

### v1.0.0 (2024-12-02)
- ✨ 初始版本发布
- 🚀 UDP 数据接收功能
- 🔄 WebSocket 数据转发
- ⚡ C++ 原生点云处理
- 📦 集成 Draco 3D 压缩库

## 📄 许可证

[添加您的许可证信息]

## 👥 联系方式

- **项目地址**: [https://github.com/caiyp7/Pointcloud](https://github.com/caiyp7/Pointcloud)
- **问题反馈**: [提交 Issue](https://github.com/caiyp7/Pointcloud/issues)
- **维护者**: [添加您的联系方式]

## ⭐ Star History

如果这个项目对你有帮助，请给个 Star ⭐️ 支持一下！

---

**最后更新**: 2024-12-02

