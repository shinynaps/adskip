# AdSkip Assist v0.2 — verification record (2026-10-12)

## Confirmed

- Source committed to [shinynaps/adskip](https://github.com/shinynaps/adskip).
- CI import/build [run 38190337510](https://github.com/shinynaps/adskip/actions/runs/38190337510) **completed successfully** with Android SDK 35, JDK 17, Gradle 8.11.1.
- `:app:testDebugUnitTest` and `:app:assembleDebug` both succeeded; the project contains **18 JUnit unit-test methods** for pure Kotlin ad matching/state machine logic.
- GitHub Actions uploaded artifact `AdSkip-Assist-v2-debug-apk` (artifact 11721228127, 36,953,102 bytes archive size). It contains the installable debug APK produced by Gradle.
- Local Kotlin rules/state-machine compilation and a separate focused sanity test also passed.

## Not verified; physical device required

- Actual installation and startup on a phone, accessibility permission behavior, screenshot permissions and screenshot-blocked windows.
- Correctness and effectiveness of on-device OCR and learned-button template matching in particular games.
- Overlay Teach UX, screen-coordinate mapping, gesture safety across resolutions/orientations, and Play Store return behavior.
- Reward credit or completion at an advertising SDK/game backend. No third-party automation can guarantee this.

## Safety and privacy boundaries

- App allowlist, explicit consent, restart-safe pause, local-only settings and activity logs.
- No root, intercepting ad network requests, modifying games, or forging reward-completion callbacks.
- Off-by-default visual OCR/template modes and store recovery. Real-game manual checks are mandatory before enabling them broadly.

## Install

Open the GitHub Actions run above, download `AdSkip-Assist-v2-debug-apk`, extract and install `app-debug.apk`. On Android enable `AdSkip Assist — selected games only` in Accessibility settings after reading the privacy disclosure.
