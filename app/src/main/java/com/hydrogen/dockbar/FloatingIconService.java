package com.hydrogen.dockbar;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

public class FloatingIconService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private SharedPreferences prefs;
    private WindowManager.LayoutParams params;

    private float initialX;
    private float initialY;
    private float initialTouchX;
    private float initialTouchY;
    
    private boolean isHidden = false;
    private int hiddenOffset = 0;
    private int edgeThreshold = 50;

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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_dockbar, null);

            int windowType;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            } else {
                windowType = WindowManager.LayoutParams.TYPE_PHONE;
            }

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);

            params.gravity = Gravity.LEFT | Gravity.TOP;
            
            int savedX = prefs.getInt("dockbar_x", 0);
            int savedY = prefs.getInt("dockbar_y", getScreenHeight() / 2 - 200);
            params.x = savedX;
            params.y = savedY;

            windowManager.addView(floatingView, params);
            applyThemeAndOpacity();
            setupButtons();
            setupDragListener();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "启动悬浮栏失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void applyThemeAndOpacity() {
        int opacity = prefs.getInt("dockbar_opacity", 80);
        int backgroundColor = getColorWithOpacity(0xFF333333, opacity);
        floatingView.setBackgroundColor(backgroundColor);
        applyIconSize();
    }

    private void applyIconSize() {
        int iconSizeProgress = prefs.getInt("icon_size_progress", 50);
        int iconSizeDp = 30 + (iconSizeProgress * 60 / 100);
        int iconSizePx = (int) (iconSizeDp * getResources().getDisplayMetrics().density);

        ImageButton[] buttons = {
                floatingView.findViewById(R.id.btn_home),
                floatingView.findViewById(R.id.btn_nav),
                floatingView.findViewById(R.id.btn_radio),
                floatingView.findViewById(R.id.btn_music),
                floatingView.findViewById(R.id.btn_phone),
                floatingView.findViewById(R.id.btn_settings),
                floatingView.findViewById(R.id.btn_app_manager)
        };

        for (ImageButton button : buttons) {
            ViewGroup.LayoutParams params = button.getLayoutParams();
            params.width = iconSizePx;
            params.height = iconSizePx;
            button.setLayoutParams(params);
            button.setScaleType(ImageButton.ScaleType.FIT_CENTER);
            button.setPadding(iconSizePx / 10, iconSizePx / 10, iconSizePx / 10, iconSizePx / 10);
        }
    }

    private int getColorWithOpacity(int baseColor, int opacityPercent) {
        int alpha = (int) (opacityPercent * 2.55);
        return (alpha << 24) | (baseColor & 0xFFFFFF);
    }

    private int getScreenHeight() {
        return getResources().getDisplayMetrics().heightPixels;
    }

    private int getScreenWidth() {
        return getResources().getDisplayMetrics().widthPixels;
    }

    private void setupDragListener() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isAtLeftEdge = false;
            private boolean isAtRightEdge = false;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isHidden) {
                            showFloatingView();
                            return true;
                        }
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        isAtLeftEdge = params.x <= 0;
                        isAtRightEdge = params.x >= getScreenWidth() - floatingView.getWidth();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (isHidden) {
                            return true;
                        }
                        params.x = (int) (initialX + event.getRawX() - initialTouchX);
                        params.y = (int) (initialY + event.getRawY() - initialTouchY);
                        
                        params.x = Math.max(0, Math.min(params.x, getScreenWidth() - floatingView.getWidth()));
                        params.y = Math.max(0, Math.min(params.y, getScreenHeight() - floatingView.getHeight()));
                        
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isHidden) {
                            return true;
                        }
                        savePosition();
                        // 检查是否拖动到边缘位置
                        if (isAtLeftEdge && params.x <= 0 && prefs.getBoolean("edge_hide", false)) {
                            hideFloatingView(true);
                        } else if (isAtRightEdge && params.x >= getScreenWidth() - floatingView.getWidth() && prefs.getBoolean("edge_hide", false)) {
                            hideFloatingView(false);
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void hideFloatingView(boolean leftSide) {
        if (isHidden) {
            return;
        }
        
        // 只留一个15dp的小条
        int tabWidth = (int) (15 * getResources().getDisplayMetrics().density);
        hiddenOffset = floatingView.getWidth() - tabWidth;
        
        if (leftSide) {
            params.x = -hiddenOffset;
        } else {
            params.x = getScreenWidth() - tabWidth;
        }
        windowManager.updateViewLayout(floatingView, params);
        isHidden = true;
    }

    private void showFloatingView() {
        if (!isHidden) {
            return;
        }
        
        if (params.x < 0) {
            params.x = 0;
        } else {
            params.x = getScreenWidth() - floatingView.getWidth();
        }
        windowManager.updateViewLayout(floatingView, params);
        isHidden = false;
    }

    private void savePosition() {
        prefs.edit()
                .putInt("dockbar_x", params.x)
                .putInt("dockbar_y", params.y)
                .apply();
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

        setAppIcon(homeBtn, homePkg, R.drawable.ic_home);
        setAppIcon(navBtn, navPkg, android.R.drawable.ic_menu_directions);
        setAppIcon(radioBtn, radioPkg, R.drawable.ic_radio);
        setAppIcon(musicBtn, musicPkg, android.R.drawable.ic_lock_silent_mode_off);
        setAppIcon(phoneBtn, phonePkg, R.drawable.ic_folder);
        setAppIcon(settingsBtn, settingsPkg, R.drawable.ic_settings);

        homeBtn.setOnClickListener(v -> handleHomeClick(homePkg));
        navBtn.setOnClickListener(v -> launchApp(navPkg));
        radioBtn.setOnClickListener(v -> launchApp(radioPkg));
        musicBtn.setOnClickListener(v -> launchApp(musicPkg));
        phoneBtn.setOnClickListener(v -> launchApp(phonePkg));
        settingsBtn.setOnClickListener(v -> launchApp(settingsPkg));
        appManagerBtn.setOnClickListener(v -> openSettingsActivity());
    }

    private void setAppIcon(ImageButton button, String packageName, int defaultResId) {
        if (packageName == null || packageName.isEmpty()) {
            button.setImageResource(defaultResId);
            return;
        }
        try {
            button.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
        } catch (Exception e) {
            button.setImageResource(defaultResId);
        }
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