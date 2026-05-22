package com.hydrogen.dockbar;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1234;

    private String pendingConfigKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean fromFloatingBar = getIntent().getBooleanExtra("from_floating_bar", false);

        if (!fromFloatingBar) {
            checkAndStartService();
            finish();
            return;
        }

        setupButtons();
    }

    private void checkAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
                return;
            }
        }
        startFloatingService();
    }

    private void startFloatingService() {
        Intent serviceIntent = new Intent(this, FloatingIconService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingService();
                Toast.makeText(this, "悬浮栏已启动", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "需要悬浮窗权限才能使用", Toast.LENGTH_SHORT).show();
            }
            finish();
        }

        if (requestCode == 5678 && resultCode == RESULT_OK && data != null) {
            ComponentName component = data.getComponent();
            if (component != null && pendingConfigKey != null) {
                String pkg = component.getPackageName();
                getSharedPreferences("dockbar_config", MODE_PRIVATE).edit()
                        .putString(pendingConfigKey, pkg).apply();
                Toast.makeText(this, "已保存: " + pkg, Toast.LENGTH_SHORT).show();
                stopService(new Intent(this, FloatingIconService.class));
                startService(new Intent(this, FloatingIconService.class));
            }
        }
    }

    private void setupButtons() {
        Button btnHome = findViewById(R.id.btn_config_home);
        Button btnNav = findViewById(R.id.btn_config_nav);
        Button btnMusic = findViewById(R.id.btn_config_music);
        Button btnPhone = findViewById(R.id.btn_config_phone);
        Button btnSettings = findViewById(R.id.btn_config_settings);
        Button btnRestart = findViewById(R.id.btn_restart_service);

        btnHome.setOnClickListener(v -> pickAppFor("home_pkg"));
        btnNav.setOnClickListener(v -> pickAppFor("nav_pkg"));
        btnMusic.setOnClickListener(v -> pickAppFor("music_pkg"));
        btnPhone.setOnClickListener(v -> pickAppFor("phone_pkg"));
        btnSettings.setOnClickListener(v -> pickAppFor("settings_pkg"));

        btnRestart.setOnClickListener(v -> {
            stopService(new Intent(this, FloatingIconService.class));
            startService(new Intent(this, FloatingIconService.class));
            Toast.makeText(this, "悬浮栏已重启", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void pickAppFor(String key) {
        pendingConfigKey = key;
        Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER));
        startActivityForResult(intent, 5678);
    }
}
