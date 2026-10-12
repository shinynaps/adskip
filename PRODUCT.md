# AdSkip Assist — Product brief

AdSkip Assist is a privacy-first Android utility for reducing repetitive tapping during **legitimately dismissible** mobile-game rewarded ads. It is not an ad-completion bypass, reward generator, traffic modifier, or reward-verification substitute.

**Audience:** people playing Android games on unrooted phones who prefer to keep ads from requiring multiple manual dismissals. **Primary moment:** the game is already showing an ad and a safe dismissal control becomes available. **Platform:** Kotlin + Jetpack Compose / Material 3, Android 8+ for basic accessibility actions, Android 11+ for screenshot recognition.

**Nonnegotiables:** explicit app allowlist and consent, instant pause, no network or accounts, no full screenshot retention, local-only logs, conservative detection, no reward claims. OCR and learned visual controls are optional. Screen capture can fail on secure surfaces. Server-side reward issuance is opaque to a third-party app.

**Navigation:** Overview (operational state + quick start), Games (allowlist and Teach), Activity (local logs), Settings (permissions, detection, behavior, emergency stop).
