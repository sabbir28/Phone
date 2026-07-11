package s28.system.phone.utils;

/**
 * Apple-Style Design System Constants
 * 
 * This class provides constants for the Apple-style dark and light mode implementation
 * used throughout the Phone application.
 * 
 * Usage:
 * - Use these constants in Java code when programmatically setting colors
 * - Use XML attributes (colors.xml, themes.xml) in layouts
 * - Follow Material Design 3 guidelines with Apple-style colors
 */
public class AppleDesignSystem {
    
    // Prevent instantiation
    private AppleDesignSystem() {
        throw new AssertionError("No instances of AppleDesignSystem");
    }
    
    /**
     * Light Mode Color Palette
     */
    public static class LightColors {
        // Primary
        public static final int PRIMARY = 0xFF007AFF;        // Apple Blue
        public static final int PRIMARY_CONTAINER = 0xFFE8F2FF;
        public static final int ON_PRIMARY = 0xFFFFFFFF;     // White text
        public static final int ON_PRIMARY_CONTAINER = 0xFF003F8F;
        
        // Secondary
        public static final int SECONDARY = 0xFF34C759;      // Apple Green
        public static final int SECONDARY_CONTAINER = 0xFFE8F9F0;
        public static final int ON_SECONDARY = 0xFFFFFFFF;   // White text
        public static final int ON_SECONDARY_CONTAINER = 0xFF1B5C2E;
        
        // Tertiary
        public static final int TERTIARY = 0xFFFF3B30;       // Apple Red
        public static final int TERTIARY_CONTAINER = 0xFFFFE8E6;
        public static final int ON_TERTIARY = 0xFFFFFFFF;    // White text
        public static final int ON_TERTIARY_CONTAINER = 0xFF8B1810;
        
        // Backgrounds
        public static final int BACKGROUND = 0xFFFFFFFF;     // White
        public static final int SURFACE = 0xFFF2F2F7;        // Light Gray
        public static final int SURFACE_VARIANT = 0xFFE5E5EA;
        public static final int ON_BACKGROUND = 0xFF000000;  // Black text
        public static final int ON_SURFACE = 0xFF000000;     // Black text
        public static final int ON_SURFACE_VARIANT = 0xFF3A3A3C;
        
        // Outlines
        public static final int OUTLINE = 0xFFD1D1D6;
        public static final int OUTLINE_VARIANT = 0xFFC7C7CC;
    }
    
    /**
     * Dark Mode Color Palette
     */
    public static class DarkColors {
        // Primary
        public static final int PRIMARY = 0xFF64B5F6;        // Bright Blue
        public static final int PRIMARY_CONTAINER = 0xFF0051C8;
        public static final int ON_PRIMARY = 0xFF001F5E;     // Dark text
        public static final int ON_PRIMARY_CONTAINER = 0xFFE8F2FF;
        
        // Secondary
        public static final int SECONDARY = 0xFF34C759;      // Apple Green (consistent)
        public static final int SECONDARY_CONTAINER = 0xFF1B5C2E;
        public static final int ON_SECONDARY = 0xFFFFFFFF;   // White text
        public static final int ON_SECONDARY_CONTAINER = 0xFFE8F9F0;
        
        // Tertiary
        public static final int TERTIARY = 0xFFFF6B63;       // Bright Red
        public static final int TERTIARY_CONTAINER = 0xFF8B1810;
        public static final int ON_TERTIARY = 0xFFFFFFFF;    // White text
        public static final int ON_TERTIARY_CONTAINER = 0xFFFFE8E6;
        
        // Backgrounds
        public static final int BACKGROUND = 0xFF000000;     // Black
        public static final int SURFACE = 0xFF1C1C1E;        // Dark Gray
        public static final int SURFACE_VARIANT = 0xFF3A3A3C;
        public static final int ON_BACKGROUND = 0xFFFFFFFF;  // White text
        public static final int ON_SURFACE = 0xFFFFFFFF;     // White text
        public static final int ON_SURFACE_VARIANT = 0xFFC7C7CC;
        
        // Outlines
        public static final int OUTLINE = 0xFF5A5A5C;
        public static final int OUTLINE_VARIANT = 0xFF424245;
    }
    
    /**
     * System Colors (consistent in both light and dark modes)
     */
    public static class SystemColors {
        public static final int SUCCESS = 0xFF34C759;   // Green
        public static final int ERROR = 0xFFFF3B30;     // Red
        public static final int WARNING = 0xFFFF9500;   // Orange
        public static final int INFO = 0xFF007AFF;      // Blue
        public static final int DESTRUCTIVE = 0xFFFF3B30; // Red
    }
    
    /**
     * Gray Palette (Apple's neutral grays)
     */
    public static class GrayPalette {
        public static final int GRAY_1 = 0xFF8E8E93;    // Secondary Gray
        public static final int GRAY_2 = 0xFFA2A2A7;    // Tertiary Gray
        public static final int GRAY_3 = 0xFFC7C7CC;    // Separator Gray
        public static final int GRAY_4 = 0xFFD1D1D6;    // Divider Gray
        public static final int GRAY_5 = 0xFFE5E5EA;    // Light Gray
        public static final int GRAY_6 = 0xFFF2F2F7;    // Very Light Gray
    }
    
    /**
     * Typography Sizes (in sp - scale-independent pixels)
     */
    public static class TypographySizes {
        // Headlines
        public static final int HEADLINE_LARGE = 32;    // Bold
        public static final int HEADLINE_MEDIUM = 28;   // Bold
        public static final int HEADLINE_SMALL = 24;    // Semibold
        
        // Titles
        public static final int TITLE_LARGE = 22;       // Semibold
        public static final int TITLE_MEDIUM = 16;      // Semibold
        public static final int TITLE_SMALL = 14;       // Medium
        
        // Body
        public static final int BODY_LARGE = 16;        // Regular
        public static final int BODY_MEDIUM = 14;       // Regular
        public static final int BODY_SMALL = 12;        // Regular
        
        // Labels
        public static final int LABEL_LARGE = 12;       // Medium
        public static final int LABEL_MEDIUM = 11;      // Medium
        public static final int LABEL_SMALL = 10;       // Medium
    }
    
    /**
     * Spacing (using 4dp base grid, Apple style)
     */
    public static class Spacing {
        public static final int XXS = 2;     // 2dp
        public static final int XS = 4;      // 4dp
        public static final int SMALL = 8;   // 8dp
        public static final int MEDIUM = 12; // 12dp
        public static final int BASE = 16;   // 16dp (standard padding)
        public static final int LARGE = 20;  // 20dp
        public static final int XL = 24;     // 24dp
        public static final int XXL = 32;    // 32dp
    }
    
    /**
     * Corner Radius (Apple-style rounded corners)
     */
    public static class CornerRadius {
        public static final int NONE = 0;
        public static final int SMALL = 2;      // 2dp - very subtle
        public static final int SMALL_MEDIUM = 4;   // 4dp
        public static final int MEDIUM = 8;     // 8dp - standard
        public static final int MEDIUM_LARGE = 12;  // 12dp
        public static final int LARGE = 16;     // 16dp
        public static final int LARGE_EXTRA = 20;   // 20dp
        public static final int FULL = 28;      // 28dp - nearly pill-shaped
    }
    
    /**
     * Component Sizes
     */
    public static class ComponentSizes {
        public static final int BUTTON_HEIGHT = 48;    // Standard button height
        public static final int BUTTON_HEIGHT_SMALL = 40;
        public static final int BUTTON_HEIGHT_TINY = 36;
        
        public static final int ICON_SMALL = 24;       // Standard icon size
        public static final int ICON_MEDIUM = 32;
        public static final int ICON_LARGE = 48;
        public static final int ICON_EXTRA_LARGE = 56;
        
        public static final int TOUCH_TARGET_MIN = 48;  // Apple's minimum touch target
    }
    
    /**
     * Elevation/Shadows (Material Design 3 compatible)
     */
    public static class Elevation {
        public static final float NONE = 0f;
        public static final float LOW = 1f;
        public static final float MEDIUM = 2f;
        public static final float MEDIUM_HIGH = 4f;
        public static final float HIGH = 6f;
        public static final float VERY_HIGH = 8f;
    }
    
    /**
     * Helper method to get color based on device dark mode
     * Note: In XML, use ?attr/colorPrimary instead
     * 
     * @param isDarkMode true for dark mode, false for light mode
     * @param lightColor color for light mode
     * @param darkColor color for dark mode
     * @return appropriate color value
     */
    public static int getAdaptiveColor(boolean isDarkMode, int lightColor, int darkColor) {
        return isDarkMode ? darkColor : lightColor;
    }
    
    /**
     * Documentation links
     * 
     * Apple Design:
     * https://developer.apple.com/design/human-interface-guidelines/
     * 
     * Material Design 3:
     * https://m3.material.io/
     * 
     * San Francisco Fonts:
     * https://developer.apple.com/fonts/
     */
}
