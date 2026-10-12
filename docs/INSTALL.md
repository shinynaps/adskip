# Install AdSkip Assist on Android

**You don't need Android Studio or a GitHub account to download the preview APK.** However, Play Protect may **block installation** because the app requires AccessibilityService. The one-tap link is not guaranteed to work on all Android devices. See [Play Protect warning and development testing](PLAY_PROTECT.md) and the [GitHub-only distribution guide](GITHUB_DISTRIBUTION.md). The app is not distributed through Google Play; developer verification and any Play Protect appeal do not create a store listing.

## 1. Download the installer

**[⬇ Download AdSkip Assist — Android APK](https://github.com/shinynaps/adskip/releases/download/preview/AdSkip-Assist-preview.apk)**

The download should be named **AdSkip-Assist-preview.apk** (not a ZIP file). If the link is not live yet, the first [GitHub Actions build](https://github.com/shinynaps/adskip/actions/workflows/android-apk.yml) may still be running. Check [Preview Release](https://github.com/shinynaps/adskip/releases/tag/preview) for availability and SHA-256 checksum.

## 2. Install it

Open the downloaded APK from your Android browser's downloads or the Files app. Android may ask you to allow installs from that app; approve only if you trust the source.

**If you see “App blocked to protect your device,” stop.** This is a Play Protect high-risk sideload block associated with sensitive accessibility permissions, **not** an ordinary install-unknown-apps prompt. Don't disable Play Protect to get past it. The [developer warning guide](PLAY_PROTECT.md) explains your options.

If Android blocks AccessibilityService activation with a **Restricted setting** warning, consult your phone's Android settings and security guidance. Some Android 13+ devices offer *Apps → AdSkip Assist → ⋮ → Allow restricted settings*. Do not disable security protections.

## 3. Grant permission and choose games

1. Open **AdSkip Assist → Settings**.
2. Read the accessibility disclosure and toggle consent.
3. Open **Android accessibility settings** and enable **AdSkip Assist — selected games only**.
4. Go to **Games** and enable only games you want monitored.
5. Go to **Home**, select **Reward Priority**, and tap **Start monitoring**.

OCR and learned targets are optional. To teach a button, tap **Teach** for an approved game and follow the in-app capture flow.

## 4. Stop monitoring

Use **Settings → Emergency stop**, or disable the Android AccessibilityService.

## Important limitations

- Preview/debug build; real game compatibility and reward preservation are unverified.
- **Skipping rewarded ads early can forfeit rewards.** The game or advertising SDK decides completion, not this app.
- Preview builds can have different debug signing keys; installing updates over an older preview can fail. Uninstalling the old build deletes saved settings and learned-button crops.
- Android 11+ is required for optional on-device screenshot recognition, and some secure screens can't be captured.
- The APK is not a Play Store release. Download only from the official [repository](https://github.com/shinynaps/adskip).
