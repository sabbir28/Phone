# Phone

## Overview

Phone is a lightweight, configurable Android dialer and calling UI designed to be a modern, Apple-style phone experience built with Material3 components and accessibility in mind. The project demonstrates a polished in-call UI, dialer, contacts, and call-log screens while providing a clean theming system (dark/light) and customizable fonts.

## Key Features

- Clean, Apple-inspired call UI and dialer screens.
- Incoming call handling via `InCallService` and a dedicated in-call activity.
- Call recording support with a visible REC control during active calls.
- Dark / Light theme support with system-aware colors.
- Custom font support (SF Pro Display & SF Pro Rounded included).
- Material3 components and accessible widgets.
- Small, modular codebase suitable for learning or theming.

## Included Resources

- Fonts: SF Pro Display & SF Pro Rounded placed under `app/src/main/res/font`.
- Drawables and layouts for dialer, in-call, contacts, and call log screens.
- Theme files in `app/src/main/res/values` and `values-night`.

## Quick Start

Prerequisites:
- Android Studio (2022.3+ recommended)
- JDK 11+
- Gradle wrapper (project includes `gradlew`)

Build and run locally:

```bash
./gradlew assembleDebug
# or open the project in Android Studio and run on an emulator/device
```

If you prefer running from the command line and installing on a connected device:

```bash
./gradlew installDebug
```

## Fonts Note

This repository includes SF Pro fonts (licensed assets). The fonts are located in `app/src/main/res/font`. If you swap fonts or update font files, update the family XMLs in the same folder: `sf_pro_display.xml` and `sf_pro_rounded.xml`.

If you previously saw resource linking errors like "resource android:font/sans_serif not found", the cause was referencing `@android:font/...`. The project now references local `@font/` files to avoid that issue.

## Troubleshooting

- Android resource linking errors: ensure any `font-family` entries reference project fonts (`@font/...`) or valid system fonts available on the device/emulator.
- Call recording requires microphone permission; the in-call screen now exposes a REC button only during active calls.
- Incoming call handling is supported by `DialerInCallService` and will show the in-call UI when a new `Call` is added.
- Lint errors related to GridLayout: use `app:layout_row` / `app:layout_column` with `androidx.gridlayout` (already fixed in `app/src/main/res/layout/activity_in_call.xml`).
- Call recording is currently a UI-level placeholder and is not fully implemented on all devices. Use caution if you expect production-ready recording behavior.
- Incoming call handling is also not fully implemented: the current UI shows in-call screens, but true telephony incoming call lifecycle and notification handling may require additional platform integration.

## Roadmap / Upcoming

Planned and suggested items:

- Add automated UI tests and instrumentation tests.
- Add CI workflow to run lint, build and unit tests on push.
- Improve accessibility (TalkBack labels, larger touch targets).
- Add runtime theme preview and theme toggles in-app.
- Provide optional configuration to load fonts from an assets bundle or remote URL.
- Implement full call recording support with permissions, audio routing, and file storage.
- Add real incoming call lifecycle handling, call notifications, and accept/reject flows.

## Contributing

- Fork the repo and open a pull request against `main`.
- Keep changes small and focused; add tests for new logic where possible.
- Update `README.md` and `IMPLEMENTATION_SUMMARY.md` with any architectural or resource changes.

## License & Fonts

- The app code is MIT-style (or choose preferred license). Place license file at project root if needed.
- SF Pro fonts are proprietary — ensure you have the right to redistribute or replace them with open alternatives when publishing.

## Contact

For questions or help, open an issue or contact the maintainer.

---

*This README was expanded automatically to provide a fuller project description, setup, and roadmap.*

