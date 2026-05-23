# Hydrogen DockBar - 桌面侧边栏

一个轻量级的 Android 悬浮快捷启动栏，在屏幕边缘提供一键启动常用应用的快捷入口，类似氢桌面的侧边栏体验。

## 功能特点

- 📱 **屏幕边缘悬浮** — 始终显示在屏幕最上层，不干扰其他应用
- 🚀 **快捷启动** — 一键启动导航、音乐、文件管理、设置等常用应用
- 🎨 **自定义应用** — 可在设置界面自由选择每个按钮对应的应用
- 🔄 **开机自启动** — 支持开机自动启动悬浮栏
- 🎯 **三种隐藏模式**：
  - **自动隐藏** — 3秒无操作自动隐藏到屏幕边缘，触摸小条重新展开
  - **手动隐藏** — 拖到屏幕边缘时自动隐藏
  - **不隐藏** — 始终完整显示
- 🎚 **可调节透明度** — 支持自定义悬浮栏透明度
- 📏 **可调节图标大小** — 支持自定义图标尺寸

## 项目结构

```
HydrogenDockBar/
├── app/
│   ├── src/main/
│   │   ├── java/com/hydrogen/dockbar/
│   │   │   ├── BootReceiver.java          # 开机自启动广播接收器
│   │   │   ├── FloatingIconService.java   # 悬浮窗核心服务
│   │   │   └── MainActivity.java          # 设置界面
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_main.xml      # 设置界面布局
│   │   │   │   └── floating_dockbar.xml   # 悬浮栏布局
│   │   │   ├── drawable/                  # 图标资源
│   │   │   ├── values/
│   │   │   │   ├── colors.xml
│   │   │   │   ├── strings.xml
│   │   │   │   └── themes.xml
│   │   │   └── mipmap-anydpi-v26/         # 启动图标
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
├── build.gradle
├── settings.gradle
└── gradlew.bat
```

## 快速开始

### 环境要求

- Android Studio Flamingo 或更高版本
- JDK 17
- Android SDK 34

### 构建

```bash
# Windows
gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

### 安装

```bash
# 通过 ADB 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 首次使用

1. 安装后打开应用，授予**悬浮窗权限**
2. 悬浮栏会自动启动，显示在屏幕边缘
3. 点击悬浮栏右侧的 **应用管理** 按钮进入设置界面
4. 可自定义：
   - 每个按钮对应的应用
   - 隐藏模式
   - 图标大小
   - 透明度

## 隐藏模式说明

| 模式 | 行为描述 |
|------|---------|
| **自动隐藏**（默认） | 悬浮栏正常显示 → 3秒无操作自动隐藏到近侧边缘（仅留15dp小条）→ 触摸小条重新展开 → 再次计时 |
| **手动隐藏** | 将悬浮栏拖动到屏幕左/右边缘时自动隐藏 → 触摸小条重新展开 → 不自动隐藏 |
| **不隐藏** | 悬浮栏始终完整显示，不会触发任何隐藏行为 |

## 技术实现

- **悬浮窗** — 使用 Android `WindowManager` + `TYPE_APPLICATION_OVERLAY` 实现
- **后台服务** — 使用 `Service` 保持悬浮栏持续运行
- **开机自启** — 使用 `BroadcastReceiver` 监听 `BOOT_COMPLETED` 广播
- **隐藏动画** — 通过修改 `WindowManager.LayoutParams.x` 实现边缘偏移隐藏
- **定时隐藏** — 使用 `Handler` + `Runnable` 实现延迟自动隐藏
- **配置存储** — 使用 `SharedPreferences` 持久化用户设置

## 最低支持

- Android 5.0 (API 21) 及以上
- 需授予 `SYSTEM_ALERT_WINDOW` 悬浮窗权限
