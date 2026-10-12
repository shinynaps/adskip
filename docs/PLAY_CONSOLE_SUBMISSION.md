# Google Play review and distribution checklist

**Status: engineering preparation only — not submitted, approved, or released through Google Play.** The current GitHub APK can still be blocked by Play Protect when downloaded from a browser.

## Why the block happens

Google identifies internet-sideloaded APKs requesting AccessibilityService as high risk. Changing an APK filename, hosting URL, signing certificate, or visual branding does **not** exempt it from Play Protect. The long-term option for convenient consumer installation is legitimate review and distribution, if the app's behavior qualifies.

- [Official Play Protect explanation](https://developers.google.com/android/play-protect/warning-dev-guidance)
- [Google Play AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Google Play 2026 API-level policy](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Android App Signing](https://developer.android.com/studio/publish/app-signing)

## Critical policy risk

Google permits **narrow, deterministic, user-defined rule automation** with AccessibilityService, but restricts apps that autonomously initiate, plan, and execute actions or decisions. The current app automatically detects and dismisses some advertising controls, sometimes using OCR and learned visual patterns. It is **not yet determined whether the full behavior satisfies that policy**. Honest disclosure and consent are necessary but do not guarantee approval.

**Never misrepresent this as a disability accessibility tool**. The service correctly declares isAccessibilityTool=false. Do not hide features from Play reviewers, forge rewards, or weaken Play Protect.

## Changes prepared

- Target Android 16 (API 36) for new mobile-app submissions after August 31, 2026.
- Display a first-launch AccessibilityService disclosure with explicit consent.
- Retain a narrowly scoped game allowlist and optional off-by-default OCR and visual matching.
- Public [privacy policy](../PRIVACY.md).
- [Play review build workflow](../.github/workflows/play-review.yml) that generates a signed Android App Bundle (.aab) and release APK after the owner provides an upload key. It **does not submit anything to Google**.

## Create and protect an upload key

Run on your trusted personal computer with Java keytool:

```bash
keytool -genkeypair -v -keystore adskip-upload.jks -alias adskip-upload -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12
```

Save the keystore and passwords offline in a secure backup. **Never commit keys or paste secrets in issues or chats.** Use an existing Play upload key if the app already has one.

In your repository **Settings → Secrets and variables → Actions**, create the following GitHub secrets:

| Secret | Meaning |
|---|---|
| PLAY_UPLOAD_KEYSTORE_B64 | Base64-encoded contents of the JKS/PKCS12 file, on one line |
| PLAY_UPLOAD_STORE_PASSWORD | Keystore password |
| PLAY_UPLOAD_KEY_ALIAS | Key alias |
| PLAY_UPLOAD_KEY_PASSWORD | Private-key password |

macOS/Linux: `base64 < adskip-upload.jks | tr -d '\n'`  
PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("adskip-upload.jks"))`

Keep the generated base64 private. Then run **Actions → Prepare signed Play Console package → Run workflow**. It fails safely if secrets are absent, and otherwise uploads a signed review bundle to **private, short-lived Actions artifacts**, not to the public GitHub Release.

## Play Console submission (owner action)

1. Create an application in [Google Play Console](https://play.google.com/console/) with package name `com.adskip.assist`.
2. Upload the signed `.aab` to a suitable testing track. Google Play App Signing manages the final distribution certificate separately from your upload key.
3. Add an accurate listing and a reachable public [privacy policy](../PRIVACY.md).
4. Complete the sensitive AccessibilityService permission declaration with an **accurate demo video**, covering consent, game allowlisting, supported close controls, permission revocation, and emergency stop.
5. Complete the Data safety and other App content forms based on real tests rather than assumptions.
6. Review Google's policy decision before wider distribution. If the current autonomous ad-close behavior is disallowed, revise the feature to narrower user-defined automation or user-confirmed actions.

**This cannot be completed solely through a GitHub commit.** It requires your developer account, signing key, truthful policy declarations, real-device testing, and approval.

## Play Protect appeal

Google offers [Play Protect classification appeals](https://developers.google.com/android/play-protect/warning-dev-guidance) after developers have established that the application is compliant and the classification is in error. An appeal is not guaranteed and is **not** a substitute for a completed code/security review. No appeal has been submitted.
