# Apple San Francisco Fonts

This directory should contain the Apple San Francisco font files. Since we're using Material Design 3 with Android fonts, 
the app will fallback to system fonts that closely resemble SF Pro.

To use actual SF Pro fonts:
1. Download SF Pro Display and SF Pro Rounded from Apple's developer website
2. Convert them to TTF or OTF format
3. Place them in this directory as:
   - sf_pro_display_regular.ttf
   - sf_pro_rounded_medium.ttf
   - sf_pro_rounded_semibold.ttf
   - sf_pro_rounded_bold.ttf

The app is configured to use these fonts, and Material 3 will provide fallback fonts if they're not present.
