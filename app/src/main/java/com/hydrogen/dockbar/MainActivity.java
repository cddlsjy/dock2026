package com.hydrogen.dockbar;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY_PERMISSION = 1234;

    private String pendingConfigKey;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("dockbar_config", MODE_PRIVATE);

        boolean fromFloatingBar = getIntent().getBooleanExtra("from_floating_bar", false);

        if (!fromFloatingBar) {
            checkAndStartService();
            finish();
            return;
        }

        setupButtons();
        setupSettings();
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
                restartService();
            }
        }
    }

    private void setupButtons() {
        Button btnHome = findViewById(R.id.btn_config_home);
        Button btnNav = findViewById(R.id.btn_config_nav);
        Button btnRadio = findViewById(R.id.btn_config_radio);
        Button btnMusic = findViewById(R.id.btn_config_music);
        Button btnPhone = findViewById(R.id.btn_config_phone);
        Button btnSettings = findViewById(R.id.btn_config_settings);
        Button btnRestart = findViewById(R.id.btn_restart_service);

        btnHome.setOnClickListener(v -> pickAppFor("home_pkg"));
        btnNav.setOnClickListener(v -> pickAppFor("nav_pkg"));
        btnRadio.setOnClickListener(v -> pickAppFor("radio_pkg"));
        btnMusic.setOnClickListener(v -> pickAppFor("music_pkg"));
        btnPhone.setOnClickListener(v -> pickAppFor("phone_pkg"));
        btnSettings.setOnClickListener(v -> pickAppFor("settings_pkg"));

        btnRestart.setOnClickListener(v -> {
            restartService();
            Toast.makeText(this, "悬浮栏已重启", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupSettings() {
        Switch edgeHideSwitch = findViewById(R.id.switch_edge_hide);
        SeekBar opacitySeekBar = findViewById(R.id.seekbar_opacity);
        TextView opacityValue = findViewById(R.id.tv_opacity_value);

        edgeHideSwitch.setChecked(prefs.getBoolean("edge_hide", false));
        opacitySeekBar.setProgress(prefs.getInt("dockbar_opacity", 80));
        opacityValue.setText(prefs.getInt("dockbar_opacity", 80) + "%");

        edgeHideSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("edge_hide", isChecked).apply();
            restartService();
        });

        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 限制最小值为 10
                if (progress < 10) {
                    seekBar.setProgress(10);
                    progress = 10;
                }
                opacityValue.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 确保不小于 10
                int progress = Math.max(seekBar.getProgress(), 10);
                prefs.edit().putInt("dockbar_opacity", progress).apply();
                restartService();
            }
        });
    }

    private void pickAppFor(String key) {
        pendingConfigKey = key;
        Intent intent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        intent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER));
        startActivityForResult(intent, 5678);
    }

    private void restartService() {
        stopService(new Intent(this, FloatingIconService.class));
        startService(new Intent(this, FloatingIconService.class));
    }
}