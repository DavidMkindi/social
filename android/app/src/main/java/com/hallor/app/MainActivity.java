package com.hallor.app;

import android.os.Bundle;
import android.content.res.Configuration;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Update system bars when theme changes
        updateSystemBars();
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
