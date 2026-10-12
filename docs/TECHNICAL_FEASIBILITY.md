# Technical feasibility, privacy, and limitations

Research reviewed 2026-10-12. This is a conservative heuristic utility, not an ad-reward bypass.

## What can and cannot be done

- **Can**: an explicit opt-in `AccessibilityService` can observe selected, accessible game windows, identify on-screen controls and call `ACTION_CLICK` on a legitimate Skip/Close control.
- **Cannot**: reliably shortcut the paid/required watch time while retaining reward eligibility. A game's ad SDK triggers completion/reward callbacks; Google AdMob and AppLovin MAX support optional signed/server-verified reward events. A separate unrooted app cannot read or force those legitimate signals across app boundaries.
- **Cannot**: reliably see Unity-rendered graphical widgets, SurfaceView content or certain WebViews when accessible labels/nodes are absent, or safely identify an anonymous X without ad context.
- **Cannot**: guarantee that the game awarded any coins/items. Both operation modes report reward verification as unknown.
- The **Reward Priority** timer (default 40s; customizable) is a *best-effort delay*, not an SDK completion signal. Ads may exceed it and rewards can still be forfeited. The completion phrases are hints, not proofs.
- The **Fast Skip** mode can forfeit rewards by closing/skipping as soon as a supported control appears.
- Some ad SDK views are in another package or system overlays: the strict allowlist intentionally refuses those windows. No coordinate/screenshot/OCR fallback is provided; false negatives are preferred over mistaken taps.
- This app doesn't automatically claim reward dialogs or press arbitrary `Continue`, `Install`, `Download` or `Play Now` controls.
- When a UI has no trustworthy ad marker, automation takes no action even if a generic X is visible.
- Android OEM variants, Android 13+ sideloaded accessibility restrictions, service restarts, battery management and app changes can cause additional compatibility issues.

## Design details

- `AccessibilityAutomationService` subscribes to window/content events; the runtime service filter is limited to selected packages. If empty, it subscribes only to its own package instead of all apps.
- `AdDetectionEngine` walks up to 350 accessible nodes for each debounced scan, requires ad-context evidence (conservative by default; optional extended matching), ignores risky CTAs, checks bounds and clickability, and never dispatches blind gestures.
- `AdStateMachine` uses elapsed real time and caps each session at five clicks, a 150-second lifetime, and 1.5-second click spacing. It needs current-frame ad evidence before acting.
- Settings live in app-private `SharedPreferences`; logs contain only a timestamp, package name, and predefined event description, limited to 150 entries. No INTERNET permission, telemetry, screenshot capture, credentials or service accounts.
- Global emergency pause is available in Home/Settings. Revoking consent disables automation. Disabling the Android accessibility service also stops it immediately.
- Only launcher apps visible to Android's package-visibility query are listed. The initial UI filters to apps classified as games, with a toggle to include misclassified games.

## Google Play considerations

Accessibility services not designed to support users with disabilities should not claim `isAccessibilityTool=true`. Google Play requires an accessibility declaration and prominent disclosure and consent, and restricts autonomous, non-deterministic automation. This app follows deterministic rules and presents disclosure and an explicit consent toggle, but **publication approval is not guaranteed**. Intended distribution is personal sideloading via Actions artifacts.

## Primary sources

- Google AdMob rewarded ads: https://developers.google.com/admob/android/rewarded
- Google AdMob SSV: https://developers.google.com/admob/android/ssv
- AppLovin MAX rewarded ads: https://developers.applovin.com/en/max/android/ad-formats/rewarded-ads/
- AppLovin S2S callback: https://support.applovin.com/en/max/advanced-features/s2s-rewarded-callback-api
- Android accessibility service: https://developer.android.com/guide/topics/ui/accessibility/views/service
- Android package visibility: https://developer.android.com/training/package-visibility/declaring
- Google Play accessibility policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Existing design references, not copied: https://github.com/zfdang/Android-Touch-Helper (MIT) and https://github.com/gkd-kit/gkd
