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
            "com.google.android.apps.maps",  // 导航
            "com.google.android.music",     // 音乐
            "com.android.dialer",            // 电话
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
        ImageButton navBtn = floatingView.findViewById(R.id.btn_nav);
        ImageButton musicBtn = floatingView.findViewById(R.id.btn_music);
        ImageButton phoneBtn = floatingView.findViewById(R.id.btn_phone);
        ImageButton settingsBtn = floatingView.findViewById(R.id.btn_settings);
        ImageButton appManagerBtn = floatingView.findViewById(R.id.btn_app_manager);

        String navPkg = prefs.getString("nav_pkg", defaultPackages[0]);
        String musicPkg = prefs.getString("music_pkg", defaultPackages[1]);
        String phonePkg = prefs.getString("phone_pkg", defaultPackages[2]);
        String settingsPkg = prefs.getString("settings_pkg", defaultPackages[3]);

        navBtn.setOnClickListener(v -> launchApp(navPkg));
        musicBtn.setOnClickListener(v -> launchApp(musicPkg));
        phoneBtn.setOnClickListener(v -> launchApp(phonePkg));
        settingsBtn.setOnClickListener(v -> launchApp(settingsPkg));
        appManagerBtn.setOnClickListener(v -> openSettingsActivity());
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
