package com.hydrogen.dockbar;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

public class FloatingIconService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private SharedPreferences prefs;

    private String[] defaultPackages = {
            "",                              // 主页（空表示使用返回主页功能）
            "com.google.android.apps.maps",  // 导航
            "com.android.providers.downloads.ui", // 收音机（默认使用下载管理器）
            "com.google.android.music",     // 音乐
            "com.google.android.documentsui", // 文件管理
            "com.android.settings"           // 设置
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("dockbar_config", MODE_PRIVATE);
        createFloatingView();
        Toast.makeText(this, "悬浮栏已启动", Toast.LENGTH_SHORT).show();
    }

    private void createFloatingView() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_dockbar, null);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
        params.x = 0;
        params.y = 0;

        windowManager.addView(floatingView, params);
        setupButtons();
    }

    private void setupButtons() {
        ImageButton homeBtn = floatingView.findViewById(R.id.btn_home);
        ImageButton navBtn = floatingView.findViewById(R.id.btn_nav);
        ImageButton radioBtn = floatingView.findViewById(R.id.btn_radio);
        ImageButton musicBtn = floatingView.findViewById(R.id.btn_music);
        ImageButton phoneBtn = floatingView.findViewById(R.id.btn_phone);
        ImageButton settingsBtn = floatingView.findViewById(R.id.btn_settings);
        ImageButton appManagerBtn = floatingView.findViewById(R.id.btn_app_manager);

        String homePkg = prefs.getString("home_pkg", defaultPackages[0]);
        String navPkg = prefs.getString("nav_pkg", defaultPackages[1]);
        String radioPkg = prefs.getString("radio_pkg", defaultPackages[2]);
        String musicPkg = prefs.getString("music_pkg", defaultPackages[3]);
        String phonePkg = prefs.getString("phone_pkg", defaultPackages[4]);
        String settingsPkg = prefs.getString("settings_pkg", defaultPackages[5]);

        homeBtn.setOnClickListener(v -> handleHomeClick(homePkg));
        navBtn.setOnClickListener(v -> launchApp(navPkg));
        radioBtn.setOnClickListener(v -> launchApp(radioPkg));
        musicBtn.setOnClickListener(v -> launchApp(musicPkg));
        phoneBtn.setOnClickListener(v -> launchApp(phonePkg));
        settingsBtn.setOnClickListener(v -> launchApp(settingsPkg));
        appManagerBtn.setOnClickListener(v -> openSettingsActivity());
    }

    private void handleHomeClick(String homePkg) {
        if (homePkg == null || homePkg.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            launchApp(homePkg);
        }
    }

    private void launchApp(String packageName) {
        Intent intent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            Toast.makeText(this, "未找到应用: " + packageName, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSettingsActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("from_floating_bar", true);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            windowManager.removeView(floatingView);
        }
        Toast.makeText(this, "悬浮栏已关闭", Toast.LENGTH_SHORT).show();
    }
}
