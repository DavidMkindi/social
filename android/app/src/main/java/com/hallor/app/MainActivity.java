package com.hallor.app;

import android.os.Bundle;
import android.content.res.Configuration;
import android.view.View;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // Update system bars based on current theme
        updateSystemBars();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Apply window insets after view is created
        getWindow().getDecorView().post(() -> applyWindowInsets());
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
                
                // Get navigation bar height
                int navigationBarHeight = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                
                // Apply bottom padding to account for system navigation bar
                // This ensures the web content (especially bottom navigation) positions above the system bar
                v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    navigationBarHeight
                );
                
                return windowInsets.toWindowInsets();
            });
            
            // Request insets to be applied
            rootView.requestApplyInsets();
        }
    }
    
    private void updateSystemBars() {
        // Get current theme (dark or light)
        int nightModeFlags = getResources().getConfiguration().uiMode & 
                            Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
        
        // Get color resource IDs
        int navBarColorResId = isDarkTheme ? 
            getResources().getIdentifier("navigation_bar_color_dark", "color", getPackageName()) :
            getResources().getIdentifier("navigation_bar_color_light", "color", getPackageName());
        
        // Set navigation bar color to match app body
        if (navBarColorResId != 0) {
            getWindow().setNavigationBarColor(ContextCompat.getColor(this, navBarColorResId));
        }
        
        // Set status bar to transparent to allow content to draw behind it
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
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
