# Privacy policy — AdSkip Assist

**Updated:** October 12, 2026  
**Maintainer:** [shinynaps](https://github.com/shinynaps)  
**Project:** [AdSkip Assist](https://github.com/shinynaps/adskip)

This describes the current open-source app; it does not imply independent certification or Google Play approval.

## Data accessed

With your explicit consent and after you turn on Android AccessibilityService, the app can inspect UI events and controls in **games you explicitly allow**. It can perform limited actions on recognized ad dismissal controls. Optional, separately enabled features may capture game screenshots for **on-device OCR** and matching learned button patterns. Optional Play Store recovery can observe redirects during recent approved ad sessions.

## Data stored on your device

- Approved game package names, user preferences, settings, consent, and counters.
- Local summaries of automation events if logging is enabled.
- Small screenshot crops of buttons you explicitly teach the app.

Full-screen teaching images are used transiently in memory and are not intentionally saved as full screenshots. Learned button crops are stored in application-private storage. The manifest disables Android automatic backup.

## Data shared externally

The app currently **does not declare the Android INTERNET permission** and has no analytics account or remote backend for accessibility information. It does not intentionally transmit screen contents or accessibility data to its maintainer. Google Play, GitHub, browsers, and operating-system services follow their own data practices when you download or install the app.

## Your choices

You can decline consent, limit which games are approved, turn optional visual recognition off, pause monitoring, delete learned button crops, clear activity logs, or disable AccessibilityService in Android Settings. Uninstalling the app removes private application data.

## Security and limitations

AccessibilityService is a highly privileged Android capability. Only install software you trust. The app is designed to avoid processing unrelated apps, but real-device behavior and third-party ad compatibility are **not independently verified**. Closing an ad early does not guarantee the game's reward.

## Contact

Open an issue in [the project tracker](https://github.com/shinynaps/adskip/issues) for privacy questions or bugs. Do not include private screenshots, login credentials, or sensitive personal data in public issues.

This policy will be updated whenever data handling changes.
