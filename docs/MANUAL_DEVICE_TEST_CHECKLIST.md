# Real-device verification checklist (not executed in this environment)

- [ ] Debug APK installs and launches on Android 14, 15 and 16 / OEM builds available for testing.
- [ ] Without explicit consent, the Start automation button stays disabled.
- [ ] Android accessibility service enables/disables correctly; UI reflects status on Resume.
- [ ] App with no allowlisted games does not monitor arbitrary packages.
- [ ] Add Game A; verify no actions in Game B, home screen, notifications or Settings.
- [ ] In Game A, an ad with accessible "Skip Ad" button is detected.
- [ ] In Reward Priority, it waits for the configured interval unless a visible completion hint is present.
- [ ] In Fast Skip, it clicks available Skip/Close control immediately.
- [ ] If countdown button is disabled, ACTION_CLICK is not called.
- [ ] Confirm video → end card → close (if each frame has explicit ad evidence).
- [ ] Confirm screen with only graphical canvas controls does *nothing*.
- [ ] Confirm "Install", "Open", "Play Now", "Learn More" are never auto-clicked.
- [ ] Switch game/activity during ad; verify automation stops.
- [ ] Pause and revoke consent mid-ad; verify automation stops.
- [ ] Compare actual in-game reward with claimed status: app must only say unknown.
- [ ] Service interruption/restart does not automatically enable automation without consent.
- [ ] Review battery usage over 30 minutes of ordinary play.

## V2 visual and UX checks (device only)

- [ ] On Android 11+ enable optional OCR; verify screenshots are never written to shared storage.
- [ ] On screenshot-protected ad, verify visual detection fails gracefully; normal interaction remains possible.
- [ ] Select an allowlisted game, press Teach, wait until an ad appears, tap CAPTURE AD and return to AdSkip.
- [ ] Screenshot correctly fits the Teach dialog and selected tap maps to physical screenshot coordinates.
- [ ] Center-screen taps cannot save templates; corner crop saves only to private app storage.
- [ ] Learned target is ignored when mode is off, when the game is not allowlisted, or when another app is on top.
- [ ] Verify gesture fallback waits for a second consistent observation before dispatching a touch.
- [ ] Disable service or press Emergency stop mid-ad and verify no further taps occur.
- [ ] Confirm Play Store recovery is disabled by default; if enabled, verify only recent detected ad redirects trigger it.
- [ ] Test tiny screen, tablet/foldable, landscape game, Android large text setting, TalkBack UI navigation.
- [ ] Confirm no reward is recorded as verified based solely on ad dismissal.
