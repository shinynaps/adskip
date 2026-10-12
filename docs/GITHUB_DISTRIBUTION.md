# GitHub-only distribution for AdSkip Assist

**Chosen distribution: GitHub Releases APK exclusively.** No Play Console, Play Store listing, Google Play testing track, or Android App Bundle (AAB) is required by this project.

## Understand the two different Google protections

### A. Play Protect's sensitive-permission installation block (the screenshot reported)

Google says that when an APK downloaded from a browser or file manager requests the **ACCESSIBILITY** capability, enhanced fraud protection in some countries can block installation automatically. This applies to GitHub Releases downloads. A new APK name, consistent signing certificate, developer verification, or a different download button is **not** a promised workaround.

After an independent source/dependency security assessment, if the app follows Google's rules and the block is believed mistaken, the maintainer may [file a Play Protect classification appeal](https://developers.google.com/android/play-protect/warning-dev-guidance). **This appeal does not require a Google Play listing**, but an appeal is not guaranteed to succeed. Never mislabel AdSkip Assist as a disability accessibility tool.

### B. Android developer verification (different requirement)

[Android Developer Console](https://developer.android.com/developer-verification/guides) is Google's dedicated console for developers distributing **only outside Google Play**. It links your developer identity with your Android package name and signing key for platform verification requirements. **It does not publish the APK and is not Play Protect's sensitive-permission appeal.** GitHub remains the sole download host.

As of October 2026, verification enforcement began for participating stores in Brazil, Indonesia, Singapore, and Thailand on September 30, 2026, and Google plans a broader global rollout in 2027. GitHub browser downloads can still encounter the separate Play Protect warning today. Google offers limited distribution (up to 20 registered devices) and full-distribution verification; select the option matching your audience.

The developer account owner must complete identity/account verification and register package `com.adskip.assist` with the **stable signing certificate**.

## Build a consistent GitHub release APK

GitHub's automatic `preview` APK is a **CI debug build** for testing, and may be signed with a different key across runs. For a consistent upgrade path, the owner should create a long-lived private signing keystore, configure GitHub Actions secrets, and manually publish versioned signed APKs.

1. On your trusted computer, generate a release key (JDK 17's `keytool`):

   ```bash
   keytool -genkeypair -v -keystore adskip-release.jks -alias adskip-release -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12
   ```

   **Back up the keystore and password securely.** Do not commit or share private keys.

2. In the GitHub repo **Settings → Secrets and variables → Actions**, add:

   | Secret | Content |
   | --- | --- |
   | `ADSKIP_RELEASE_KEYSTORE_B64` | Base64 of the keystore file (single line) |
   | `ADSKIP_RELEASE_STORE_PASSWORD` | Keystore password |
   | `ADSKIP_RELEASE_KEY_ALIAS` | Signing-key alias |
   | `ADSKIP_RELEASE_KEY_PASSWORD` | Private-key password |

   macOS/Linux: `base64 < adskip-release.jks | tr -d '\n'`

   PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("adskip-release.jks"))`

3. Ensure `versionName = "0.3.0"` and `versionCode = 3` in `app/build.gradle.kts` for the first `v0.3.0` release. **Increase versionCode and versionName** for subsequent releases.

4. Run **Actions → Publish GitHub-only signed APK → Run workflow**, with tag `v0.3.0`. It validates the version, runs tests, builds a signed `AdSkip-Assist.apk`, verifies the signature, generates checksums, and publishes a **GitHub pre-release**. It requires the secrets and will refuse an existing version tag.

5. Link users to the GitHub Release after verifying device behavior. The ZIP-free APK is the release asset. The certificate digest in `signature-info.txt` is useful for Android Developer Console registration, and the SHA-256 checksum lets users check download integrity.

**Caveat:** A previously installed CI-debug APK cannot necessarily be updated by the developer-signed version in place. Switching certificates may require uninstalling the old build and losing its local data. Future versions signed with the same release key can update more consistently.

## GitHub public download policy

- GitHub Releases is the only artifact-distribution destination.
- Automatic CI preview APKs remain explicitly labeled **testing only**.
- Manually published stable-key APKs are **pre-releases** until tested with actual ad screens.
- No automatic Google Play submission, no false claim of Play Protect approval, and no installer security bypass.
- Report installation blocks with the Android device model, Android version, country, screenshot with personal information removed, the downloaded release tag, and reproducible steps.

## Review before appealing

Google requires the app and included SDKs to be free of malicious or unwanted behavior. Review the full accessibility-service source, dependency provenance, screenshot handling, local data retention, code signing, and device behavior. The existing tests and manifest check are useful but **not an independent safety audit**.

[Google's developer warning and appeal guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).
