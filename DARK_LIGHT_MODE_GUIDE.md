# Apple-Style Dark & Light Mode Implementation Guide

## Overview
Your Phone application now has a complete Apple-style dark and light mode implementation with San Francisco-inspired typography.

## Architecture

### Color System (Material Design 3 Compatible)

#### Light Mode Colors:
- **Primary**: `#007AFF` (Apple Blue)
- **Primary Container**: `#E8F2FF`
- **Secondary**: `#34C759` (Apple Green)
- **Tertiary**: `#FF3B30` (Apple Red)
- **Surface**: `#F2F2F7` (Light Gray)
- **Background**: `#FFFFFF` (White)

#### Dark Mode Colors:
- **Primary**: `#64B5F6` (Bright Blue for dark mode)
- **Primary Container**: `#0051C8`
- **Secondary**: `#34C759` (Green stays consistent)
- **Tertiary**: `#FF6B63` (Bright Red for dark mode)
- **Surface**: `#1C1C1E` (Dark Gray - Apple's standard)
- **Background**: `#000000` (Pure Black)

### Typography

All text in the app now uses **San Francisco-inspired fonts** through Material Design 3:

#### Font Families:
1. **SF Pro Display** - For body text and regular content
2. **SF Pro Rounded** - For headlines, titles, labels, and UI elements

#### Text Styles:
- **HeadlineLarge**: 32sp (Bold) - Main headings
- **HeadlineMedium**: 28sp (Bold) - Secondary headings
- **HeadlineSmall**: 24sp (Semibold) - Tertiary headings
- **TitleLarge**: 22sp (Semibold) - List titles
- **TitleMedium**: 16sp (Semibold) - Card titles
- **TitleSmall**: 14sp (Medium) - Section titles
- **BodyLarge**: 16sp (Regular) - Long text content
- **BodyMedium**: 14sp (Regular) - Standard body text
- **BodySmall**: 12sp (Regular) - Secondary text
- **LabelLarge**: 12sp (Medium) - Button text
- **LabelMedium**: 11sp (Medium) - Chips, badges
- **LabelSmall**: 10sp (Medium) - Helper text

### File Structure

```
res/
├── values/
│   ├── colors.xml              # Light mode colors
│   ├── themes.xml              # Light mode theme
│   ├── text_appearance.xml     # Text styles
│   ├── widget_styles.xml       # Component styles
│   └── dimens.xml              # Spacing & dimensions
├── values-night/
│   ├── colors.xml              # Dark mode color overrides
│   └── themes.xml              # Dark mode theme
├── font/
│   ├── sf_pro_display.xml      # Font family definition
│   ├── sf_pro_rounded.xml      # Font family definition
│   └── README.md               # Font installation guide
└── layout/
    └── *.xml                   # Layouts use text appearance styles
```

## Usage in Layouts

### Using Material Design 3 Attributes:
```xml
<!-- Use Material 3 color attributes -->
<view
    android:background="?attr/colorSurface"
    android:textColor="?attr/colorOnBackground" />

<!-- Use Material 3 text appearances -->
<TextView
    android:textAppearance="@style/TextAppearance.Phone.BodyMedium" />
```

### Using Custom Widget Styles:
```xml
<!-- Buttons -->
<Button
    style="@style/Widget.Phone.Button.Primary"
    android:text="Action" />

<Button
    style="@style/Widget.Phone.Button.Secondary"
    android:text="Cancel" />

<!-- Cards -->
<com.google.android.material.card.MaterialCardView
    style="@style/Widget.Phone.CardView" />

<!-- FloatingActionButton -->
<com.google.android.material.floatingactionbutton.FloatingActionButton
    style="@style/Widget.Phone.FloatingActionButton" />
```

## Dark/Light Mode Switch

Android automatically switches themes based on system settings:

1. **Light Mode**: Enabled when device setting is "Light" or "Battery Saver"
2. **Dark Mode**: Enabled when device setting is "Dark"

No code changes needed! Android automatically:
- Applies values-night resources
- Switches all `?attr/` color references
- Updates status bar appearance

## Adding San Francisco Fonts

To use actual Apple San Francisco fonts:

1. Download from Apple's official source:
   - SF Pro Display: https://developer.apple.com/fonts/
   - SF Pro Rounded: https://developer.apple.com/fonts/

2. Convert to TTF/OTF format if needed

3. Place files in `app/src/main/res/font/`:
   - `sf_pro_display_regular.ttf`
   - `sf_pro_rounded_medium.ttf`
   - `sf_pro_rounded_semibold.ttf`
   - `sf_pro_rounded_bold.ttf`

The app will automatically use these fonts with fallback to system fonts.

## Key Features

✅ **Perfect Dark/Light Mode**
- Automatic system theme detection
- Precise Apple-style colors
- No manual dark mode handling needed

✅ **Apple-Style Typography**
- SF Pro fonts for native feel
- Consistent typography hierarchy
- Optimized line heights

✅ **Material Design 3 Compatibility**
- Modern Material 3 components
- Dynamic color support (Android 12+)
- Accessibility optimized

✅ **Adaptive Components**
- BottomNavigationView with theme support
- MaterialCardView with proper elevation
- Material buttons with correct styling

✅ **Proper Spacing & Dimensions**
- Apple-style 8pt grid system
- Consistent corner radius values
- Proper touch target sizes

## Customization

### Changing Primary Color:
Edit `values/colors.xml` and `values-night/colors.xml`:
```xml
<color name="light_primary">#YOUR_COLOR</color>
<color name="dark_primary">#YOUR_BRIGHT_COLOR</color>
```

### Adding Custom Text Style:
Add to `values/text_appearance.xml`:
```xml
<style name="TextAppearance.Phone.Custom" parent="TextAppearance.Material3.BodyMedium">
    <item name="fontFamily">@font/sf_pro_display</item>
    <item name="android:textSize">14sp</item>
</style>
```

### Changing Shape Appearance:
Edit `values/themes.xml`:
```xml
<style name="ShapeAppearance.Phone.SmallComponent">
    <item name="cornerSize">12dp</item>  <!-- Increase corner radius -->
</style>
```

## Testing

### Test Dark Mode:
1. Android Settings → Display → Dark theme
2. Or use Developer Options → Force dark mode

### Test Light Mode:
1. Android Settings → Display → Light theme

### Test Accessibility:
- Check text contrast in both modes
- Verify text sizes are readable
- Test with accessibility services enabled

## Performance Notes

- Colors are static, no runtime overhead
- Fonts use system caching
- Theme resources are lightweight
- No additional dependencies required

## Browser Compatibility

All changes are Android-specific and don't affect web versions.

---

**Setup Complete!** Your Phone application now has professional Apple-style theming.
