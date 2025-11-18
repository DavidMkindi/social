package com.hallor.app;

import android.os.Bundle;
import android.content.res.Configuration;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebResourceError;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.net.Network;
import android.content.Context;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private boolean isOnline = true;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // Initialize network monitoring
        initializeNetworkMonitoring();
        
        // Update system bars based on current theme
        updateSystemBars();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Apply window insets after view is created
        getWindow().getDecorView().post(() -> {
            applyWindowInsets();
            configureWebViewCaching();
        });
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Check network state when app resumes
        checkNetworkState();
        // Update cache mode based on current network state
        getWindow().getDecorView().post(() -> {
            try {
                WebView webView = getBridge().getWebView();
                if (webView != null) {
                    android.webkit.WebSettings settings = webView.getSettings();
                    updateCacheMode(settings);
                }
            } catch (Exception e) {
                // Silently handle errors
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister network callback
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                // Ignore if already unregistered
            }
        }
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Update system bars when theme changes
        updateSystemBars();
        // Reapply window insets
        applyWindowInsets();
    }
    
    private void applyWindowInsets() {
        // Get the root view of the activity
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                WindowInsetsCompat windowInsets = WindowInsetsCompat.toWindowInsetsCompat(insets);
                
                // Get status bar height for top padding
                int statusBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                
                // Get navigation bar height for bottom padding
                int navigationBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                
                // Apply top padding to account for system status bar
                // This ensures the header positions below the status bar
                // Apply bottom padding to account for system navigation bar
                // This ensures the web content (especially bottom navigation) positions above the system bar
                v.setPadding(
                    v.getPaddingLeft(),
                    statusBarHeight,
                    v.getPaddingRight(),
                    navigationBarHeight
                );
                
                return windowInsets.toWindowInsets();
            });
            
            // Request insets to be applied
            rootView.requestApplyInsets();
        }
    }
    
    private void initializeNetworkMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    isOnline = true;
                    // When online, update cache mode and reload to get fresh content
                    runOnUiThread(() -> {
                        WebView webView = getBridge().getWebView();
                        if (webView != null) {
                            android.webkit.WebSettings settings = webView.getSettings();
                            updateCacheMode(settings);
                            // Reload to fetch new content
                            webView.reload();
                        }
                    });
                }
                
                @Override
                public void onLost(Network network) {
                    isOnline = false;
                    // When offline, switch to cache-only mode
                    runOnUiThread(() -> {
                        WebView webView = getBridge().getWebView();
                        if (webView != null) {
                            android.webkit.WebSettings settings = webView.getSettings();
                            updateCacheMode(settings);
                        }
                    });
                }
            };
            
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
        
        // Initial network state check
        checkNetworkState();
    }
    
    private void checkNetworkState() {
        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                isOnline = capabilities != null && 
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                     capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                isOnline = networkInfo != null && networkInfo.isConnected();
            }
        }
    }
    
    private void configureWebViewCaching() {
        try {
            WebView webView = getBridge().getWebView();
            if (webView != null) {
                android.webkit.WebSettings settings = webView.getSettings();
                
                // Enable aggressive caching
                // When online: LOAD_DEFAULT (load from network and cache)
                // When offline: LOAD_CACHE_ELSE_NETWORK (use cache, never show error)
                updateCacheMode(settings);
                
                // Enable DOM storage and database for caching
                settings.setDomStorageEnabled(true);
                settings.setDatabaseEnabled(true);
                
                // Enable JavaScript and other essential settings
                settings.setJavaScriptEnabled(true);
                settings.setJavaScriptCanOpenWindowsAutomatically(true);
                settings.setLoadsImagesAutomatically(true);
                settings.setBlockNetworkImage(false);
                settings.setBlockNetworkLoads(false);
                
                // Enable mixed content for better caching
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    settings.setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                }
                
                // Set custom WebViewClient to handle offline scenarios
                webView.setWebViewClient(new OfflineCapableWebViewClient());
            }
        } catch (Exception e) {
            // Silently handle any errors
        }
    }
    
    private void updateCacheMode(android.webkit.WebSettings settings) {
        if (settings != null) {
            if (isOnline) {
                // When online: load from network and cache for future use
                settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
            } else {
                // When offline: use cache, never show error page
                settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);
            }
        }
    }
    
    private class OfflineCapableWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            // Never show error page - always try to load from cache
            // This ensures users always see cached content instead of error messages
            if (request.isForMainFrame()) {
                // For main frame errors, try to reload from cache
                if (!isOnline) {
                    // When offline, force cache usage
                    android.webkit.WebSettings settings = view.getSettings();
                    settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ONLY);
                    view.reload();
                    // Reset cache mode after reload attempt
                    settings.setCacheMode(android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK);
                }
            }
            // Don't call super to prevent default error page from showing
        }
        
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            // Continue loading even on HTTP errors to show cached content
            // Only show error for non-cached resources when online
            if (isOnline) {
                super.onReceivedHttpError(view, request, errorResponse);
            }
            // When offline, ignore HTTP errors to show cached content
        }
        
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            // If offline, try to serve from cache
            if (!isOnline) {
                // Return null to let WebView use cached content
                // This allows the WebView to automatically serve from cache
                return null;
            }
            // When online, use default behavior (load from network and cache)
            return super.shouldInterceptRequest(view, request);
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // Always allow navigation, even when offline
            // This ensures smooth navigation through cached content
            return false;
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // Ensure cache mode is set correctly after page loads
            android.webkit.WebSettings settings = view.getSettings();
            updateCacheMode(settings);
        }
    }
    
    private void updateSystemBars() {
        // Get current theme (dark or light)
        int nightModeFlags = getResources().getConfiguration().uiMode & 
                            Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
        
        // Get color resource IDs for navigation bar
        int navBarColorResId = isDarkTheme ? 
            getResources().getIdentifier("navigation_bar_color_dark", "color", getPackageName()) :
            getResources().getIdentifier("navigation_bar_color_light", "color", getPackageName());
        
        // Get color resource IDs for status bar (same as navigation bar)
        int statusBarColorResId = isDarkTheme ? 
            getResources().getIdentifier("status_bar_color_dark", "color", getPackageName()) :
            getResources().getIdentifier("status_bar_color_light", "color", getPackageName());
        
        // Set navigation bar color to match app body
        if (navBarColorResId != 0) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, navBarColorResId));
        }
        
        // Set status bar color to match navigation bar (same background color)
        if (statusBarColorResId != 0) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, statusBarColorResId));
        } else {
            // Fallback: use navigation bar color if status bar color not found
            if (navBarColorResId != 0) {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, navBarColorResId));
            }
        }
        
        // Set status bar and navigation bar icon colors based on theme
        WindowInsetsControllerCompat windowInsetsController = 
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            // Light icons on dark background, dark icons on light background
            windowInsetsController.setAppearanceLightStatusBars(!isDarkTheme);
            windowInsetsController.setAppearanceLightNavigationBars(!isDarkTheme);
        }
    }
}
