# AdSkip Assist

An Android utility for automatically pressing **legitimate, accessible** ad dismissal controls in **explicitly selected games**. Kotlin + Jetpack Compose + Material 3 + MVVM; no root, no ads SDK injection, no traffic interception, no account login and no INTERNET permission.

**Important:** This does not bypass rewarded-ad requirements or guarantee rewards. A separate Android app cannot reliably detect or forge a game's reward-verification callbacks. "Reward Priority" waits at least a configurable time or until a visible completion *hint* appears; either condition can still be insufficient for rewards. "Fast Skip" may deliberately forfeit rewards. See [technical feasibility](docs/TECHNICAL_FEASIBILITY.md).

## Features

- Home screen with pause/resume, accessibility status, selected-game count, ad-session count and accepted-click count.
- Games screen: searchable launcher apps, per-game enable/disable, operating mode override, and minimum-wait override.
- Activity screen: local-only history, clear button. Never logs captured accessibility content.
- Settings: explicit consent, conservative/extended detection, default timing, log toggle, emergency stop, Android accessibility management.
- Native AccessibilityService with *current-frame ad-context requirement*, clickability/bounds checks, risky-CTA exclusion, event debounce, multi-stage state tracking, capped retries and timeouts.
- GitHub Actions workflow builds and uploads a debug APK without Android Studio.

## Get an APK without Android Studio

1. Create a new GitHub repository and add the **contents** of this project (including `.github/workflows/android-apk.yml`) to its default `main` branch.
2. Open the **Actions** tab in your repository. Select **Build Android APK**, then click **Run workflow** (or push a commit to `main`).
3. When the job is green, open the run and download **AdSkip-Assist-debug-apk** under **Artifacts**.
4. Extract the ZIP; the file inside is `app-debug.apk`. Copy it to your Android phone and open it to install.
5. Android may require **Install unknown apps** permission for the Files/browser app used to install it. Only install builds from repos you control and trust.
6. Open AdSkip Assist > **Settings**; read the disclosure and enable **I understand and consent**.
7. Tap **Manage accessibility access**, locate **AdSkip Assist — selected games only** in Android Accessibility settings, and enable it.
8. Back in AdSkip Assist > **Games**, choose games. Turn off the "Game-classified apps only" filter if your game is missing.
9. Back on **Home**, select Reward Priority (recommended initially) or Fast Skip, then tap **Start automation**.

**Android 13+ sideload note:** Your device may prevent a sideloaded app from enabling accessibility with a "Restricted setting" warning. Some phones allow this through **Settings > Apps > AdSkip Assist > ⋮ > Allow restricted settings**. OEM behavior differs; don't disable security scanning or protections to bypass restrictions.

## Local build

Requirements: Java 17, Android command-line SDK (`platforms;android-35`, `build-tools;35.0.0`), and Gradle 8.11.1. Android Studio is optional.

```bash
sdkmanager "platforms;android-35" "build-tools;35.0.0"
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
# resulting APK: app/build/outputs/apk/debug/app-debug.apk
```

`gradle/wrapper/gradle-wrapper.properties` is supplied, but this source archive doesn't contain a Gradle wrapper JAR. Use installed Gradle 8.11.1, or run `gradle wrapper --gradle-version 8.11.1` to generate standard wrapper files. The GitHub Actions workflow installs Gradle itself.

## Testing

- Unit tests cover time gates, Fast Skip, completion hints, missing controls, cross-app changes, debounce/loop cap, and inactive-ad behavior.
- CI runs tests and an Android debug build. **CI green != device verified.**
- Run manual tests on an Android 14/15/16 device using an allowed test game. Verify pause, the game allowlist, timer, ad detections, window changes, repeated ads and that only Close/Skip buttons are tapped.
- Verify the actual game reward **inside the game**; AdSkip Assist cannot confirm it. If a specific ad never closes, its controls may be inaccessible or fail the safety criteria.

## Troubleshooting

| Symptom | Explanation / next action |
| --- | --- |
| Not active | Check consent, Android accessibility enabled, allowed game selected, and Start automation. |
| Game missing | Switch off "Game-classified apps only" and search by label/package. |
| Ad not detected | Some SDKs use entirely graphical controls, or an ad is missing strong labels; safety defaults to no click. |
| Ad closes without reward | Reward Priority waits a *minimum*, not guaranteed duration. Increase per-game wait; test the game's legitimate completion UI. |
| Android disallows accessibility | Android 13+ "Restricted settings" protection may apply to sideloads; see installation steps. |
| UI closes wrongly | Immediately use Emergency stop, remove game from allowlist, and file a reproducible issue without personal screen data. |
| No APK artifact | Inspect the Actions build log. Dependencies require GitHub runner internet; CI must finish successfully. |
| Build fails locally | Confirm Gradle 8.11.1, Java 17, Android SDK 35 and repository access. |

## Privacy & policy

This app has no INTERNET permission and runs locally. Accessibility permission is extremely sensitive. Review the source before enabling. The service is used for narrow, deterministic, opt-in automation and **does not claim to be a disability accessibility tool**. Google Play distribution would require independent policy review; publication approval is not promised.

## Project structure

- `app/src/main/java/com/adskip/assist/automation/`: scanner, finite-state engine, accessibility service.
- `app/src/main/java/com/adskip/assist/data/`: app-private settings, event logs, statistics.
- `app/src/main/java/com/adskip/assist/MainActivity.kt`: four Compose screens.
- `app/src/test/`: JVM unit tests for the state machine.
- `.github/workflows/android-apk.yml`: Android APK build, test and downloadable CI artifact.
- `docs/TECHNICAL_FEASIBILITY.md`: research and unavoidable constraints.

## Optional GitHub Releases

Push a version tag such as `v0.1.0` to trigger `.github/workflows/release-preview.yml`. It builds the debug APK and creates a **draft prerelease**; review it and publish manually if desired. Debug signing is strictly for testing. No production signing secrets are stored in the project.

### Upload this source ZIP into a new repository using Git (desktop only, no Android Studio)

Create an empty GitHub repository named `adskip-assist` first. Extract this ZIP on your desktop, then from the extracted `AdSkip-Assist` folder run:

```bash
git init -b main
git add .
git commit -m "Initial AdSkip Assist source"
git remote add origin https://github.com/YOUR-USERNAME/adskip-assist.git
git push -u origin main
```

Replace `YOUR-USERNAME` with your GitHub username. GitHub Actions then builds the APK online. Keep the `.github` directory in the repository; it holds both workflows.
