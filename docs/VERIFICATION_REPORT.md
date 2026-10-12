# Verification performed 2026-10-12

## Completed

- Manually reviewed project layout, manifest, permissions, and Gradle/CI configuration.
- Reviewed official AdMob SSV/rewarded, AppLovin S2S, Android Accessibility and Google Play policy documentation.
- Compiled `AdStateMachine.kt` and `AdUiRules.kt` using the installed local Kotlin/JVM compiler (1.9.0).
- Executed all **17** source unit-test methods from `AdStateMachineTest.kt` and `AdUiRulesTest.kt` with a local JUnit-compatible shim because JUnit libraries/Gradle were not installed. All 17 assertions/test methods completed successfully.

## Not completed / unavailable

- Android Gradle Plugin and Android SDK were unavailable in the execution environment, and it cannot resolve the external artifact repositories. Thus `:app:assembleDebug` and `:app:testDebugUnitTest` could NOT be executed here.
- No installable APK was generated in this environment; the Actions workflows must compile one after the source is pushed into a repository.
- No Android emulator or physical device was connected. Accessibility UI behavior, actual ad SDK integration, APK installation, and real rewards remain untested.
- No GitHub repository was created or uploaded. Source is provided as a ZIP and ready for the user to upload.

## Scope of results

The 17 passing tests validate only pure Kotlin automation decisions and text/control recognition heuristics. They do **not** establish that the Android app compiles or that it works in any particular game. Do not use this report as proof of device compatibility or reward preservation.
