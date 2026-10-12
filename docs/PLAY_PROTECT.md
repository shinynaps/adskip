# Google Play Protect blocks the APK

## “App blocked to protect your device”

If you see this message while opening `AdSkip-Assist-preview.apk`:

> **App blocked to protect your device**  
> This app can request access to sensitive data. This can increase the risk of identity theft or financial fraud.

This is a **Google Play Protect enhanced fraud protection** block, not necessarily a build failure and not proof by itself that the app contains malware. Google's documentation explicitly associates this warning with **internet-sideloaded apps** that declare sensitive SMS, notification listener, or accessibility permissions.

- [Google's exact warning and developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)
- [How Play Protect protects devices](https://support.google.com/android/answer/2812853)
- [Google Play accessibility API policy](https://support.google.com/googleplay/android-developer/answer/10964491)

## Why this project triggers the protection

AdSkip Assist includes an `AccessibilityService` that reads accessible window content, observes window changes, can capture supported screen screenshots, and dispatches touch gestures. Those capabilities are fundamental to its opt-in ad-close automation, **and they are highly sensitive**.

The Android manifest does **not** currently declare `INTERNET`, `READ_SMS`, or `RECEIVE_SMS`. The accessibility service config correctly uses `isAccessibilityTool="false"` because this project is not primarily an aid for people with disabilities.

- Do **not** disable Play Protect or system security scanning to install this app.
- Do **not** mislabel this project as a disability accessibility tool to avoid warnings.
- Changing the download filename, repackaging the APK, or showing a different GitHub link is **not** a legitimate fix.
- Removing the accessibility service would also remove the core ad-automation functionality.
- Android's separate **“Restricted setting”** dialog is **not the same** as this Play Protect installation block.

## Responsible next steps

### For people who just want to install it

This preview does **not** currently offer a reliable, Play-Protect-approved one-tap installation route in all regions. If your phone blocks the APK, **keep Play Protect on and do not proceed through any bypass tutorial**. You can review the project's public source code and watch the [GitHub Releases](https://github.com/shinynaps/adskip/releases) for a future approved distribution.

### For the developer

Use the [Google Play review preparation checklist](PLAY_CONSOLE_SUBMISSION.md). The repository targets API 36 and can build privately signed review artifacts once the owner sets the required GitHub Actions secrets. It has **not** been approved or published through Google Play, and signed APKs can still trigger Play Protect when internet-sideloaded.


1. Review the complete app code and dependencies for unintended access, leaks, and excessive privilege. An absent INTERNET permission is a helpful limitation but **not a complete security audit**.
2. Keep the AccessibilityService limited to explicitly approved games, require clear disclosure and consent, and maintain the emergency stop.
3. Consider publishing through a distribution channel with appropriate review, including Google Play testing **only after accessibility-policy review**. Google Play approval is **not guaranteed** for an ad automation app.
4. If the implementation and distribution are compliant and you believe Play Protect classified it incorrectly, follow [Google's Play Protect appeal process](https://developers.google.com/android/play-protect/warning-dev-guidance).
5. For your own **controlled developer testing**, Android officially supports installing locally built APKs from a connected development computer using **Android SDK Platform-Tools (ADB)**, without Android Studio. This is a development workflow, **not a consumer installation recommendation or a guarantee that security checks will allow it**. Do not override a device's protection if it still blocks the package.

See [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools) and the official [Android Debug Bridge installation guide](https://developer.android.com/tools/adb).

## Test-build limitations

The APK is currently debug-signed by GitHub Actions, with signing keys that may differ between builds. This may prevent seamless updates; uninstalling an old debug build erases local settings and taught controls. Production distribution requires a persistent secure release-signing key and an approved distribution plan.

An APK building successfully does **not** establish that it is safe, accepted by Play Protect, compatible with your phone, or able to preserve rewarded-ad credit.
