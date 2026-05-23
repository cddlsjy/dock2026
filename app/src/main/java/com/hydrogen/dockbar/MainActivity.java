package com.hydrogen.dockbar;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
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
        Button btnNav = findViewById(R.id.btn_config_nav);
        Button btnRadio = findViewById(R.id.btn_config_radio);
        Button btnMusic = findViewById(R.id.btn_config_music);
        Button btnPhone = findViewById(R.id.btn_config_phone);
        Button btnRestart = findViewById(R.id.btn_restart_service);

        btnNav.setOnClickListener(v -> pickAppFor("nav_pkg"));
        btnRadio.setOnClickListener(v -> pickAppFor("radio_pkg"));
        btnMusic.setOnClickListener(v -> pickAppFor("music_pkg"));
        btnPhone.setOnClickListener(v -> pickAppFor("phone_pkg"));

        btnRestart.setOnClickListener(v -> {
            restartService();
            Toast.makeText(this, "悬浮栏已重启", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupSettings() {
        Spinner hideModeSpinner = findViewById(R.id.spinner_hide_mode);
        SeekBar opacitySeekBar = findViewById(R.id.seekbar_opacity);
        SeekBar iconSizeSeekBar = findViewById(R.id.seekbar_icon_size);
        TextView opacityValue = findViewById(R.id.tv_opacity_value);
        TextView iconSizeValue = findViewById(R.id.tv_icon_size_value);

        String hideMode = prefs.getString("hide_mode", "auto");
        String[] modes = {"auto", "manual", "none"};
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(hideMode)) {
                hideModeSpinner.setSelection(i);
                break;
            }
        }

        opacitySeekBar.setProgress(prefs.getInt("dockbar_opacity", 80));
        iconSizeSeekBar.setProgress(prefs.getInt("icon_size_progress", 50));
        opacityValue.setText(prefs.getInt("dockbar_opacity", 80) + "%");
        iconSizeValue.setText(getIconSizeDp(prefs.getInt("icon_size_progress", 50)) + "dp");

        hideModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] modes = {"auto", "manual", "none"};
                prefs.edit().putString("hide_mode", modes[position]).apply();
                restartService();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacityValue.setText(progress + "%");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("dockbar_opacity", seekBar.getProgress()).apply();
                restartService();
            }
        });

        iconSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                iconSizeValue.setText(getIconSizeDp(progress) + "dp");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("icon_size_progress", seekBar.getProgress()).apply();
                restartService();
            }
        });
    }

    private int getIconSizeDp(int progress) {
        // 进度 0-100 对应 30dp-90dp
        return 30 + (progress * 60 / 100);
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