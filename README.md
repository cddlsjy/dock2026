# 桌面侧边栏 (Hydrogen DockBar)

一个简单的Android应用，在屏幕左侧显示永久悬浮的快捷启动栏，类似氢桌面的效果。

## 功能特点

- 📱 左侧永久悬浮的图标栏
- 🚀 一键启动常用应用（导航、音乐、电话、设置）
- 🔄 开机自动启动
- 💫 轻量简洁，不占用系统资源

## 项目结构

```
f:\build_workspace\05-21\
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/hydrogen/dockbar/
│   │       │   ├── BootReceiver.java        # 开机自启动接收器
│   │       │   ├── FloatingIconService.java # 悬浮窗服务
│   │       │   └── MainActivity.java        # 主活动
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   ├── activity_main.xml
│   │       │   │   └── floating_dockbar.xml
│   │       │   └── values/
│   │       │       ├── colors.xml
│   │       │       ├── strings.xml
│   │       │       └── themes.xml
│   │       └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── gradle/
├── build.gradle
├── settings.gradle
└── gradlew.bat
```

## 使用说明

### 1. 打开项目

使用 Android Studio 打开此项目。

### 2. 构建和安装

```bash
# Windows系统
gradlew.bat assembleDebug

# 或者在Android Studio中直接点击 Run 按钮
```

### 3. 授予权限

首次运行时，需要授予"悬浮窗"权限。

### 4. 自定义应用包名

在 `FloatingIconService.java` 中修改 `launchApp()` 调用的包名，以启动您想要的应用：

```java
navBtn.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {
        launchApp("com.your.app.package"); // 修改为您的应用包名
    }
});
```

## 技术实现

- 使用 Android 原生的 `WindowManager` 实现悬浮窗
- 使用 `Service` 实现后台运行
- 使用 `BroadcastReceiver` 实现开机自启动
- 最低支持 Android 5.0 (API 21)

## 注意事项

- 需要授予悬浮窗权限才能正常使用
- 部分定制Android系统可能需要额外的自启动权限设置
