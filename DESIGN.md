# Design system: Quiet Arcade Instrument

## Overview
A premium technical companion for gaming, balancing fast-read status with accessible controls. Translation of [Impeccable](https://github.com/pbakaus/impeccable) and [Taste Skill](https://github.com/leonxlnx/taste-skill) principles into native Jetpack Compose rather than attempting to run web-specific code in Android.

## Colors
- **Signal lime** `#D9F96B`: limited to high-value active states and primary actions.
- **Night field** `#0C171B`: main background; tinted, not pure black.
- **Raised field** `#14252A`, `#1D3236`: functional grouping.
- **Chalk** `#F0F4EC`: primary copy.
- **Sea glass** `#A8BCB9`: secondary copy.
- **Aqua** `#7DD9D3`: neutral activity markers.
- **Coral** `#FF9B8B`: paused/warning states.

**Signal-is-scarce rule:** keep bright lime for things users need to act on or visually parse instantly; no lime gradients.

## Typography
Display: native Sans Serif SemiBold with tight tracking, 39sp/41sp on Overview. Titles: 26sp, 19sp. Body: 12–14sp with 18–21sp line height. Metadata: 10–11sp monospace uppercase letter-spaced. Honor system font scaling; avoid clipping critical text.

## Layout
A vertical mobile flow. Main horizontal spacing 20dp; panel inset 20dp; consistent 4/8/12/18/22 spacing rhythm. Meaningful negative space between status, metrics, configuration. Primary CTA full width at 52dp minimum touch height. Navigation bottom with four explicit destinations.

## Elevation & Depth
Mostly flat tonal depth; nested cards avoided. One decisive raised status surface, thin outlined metric cells, separator rows for lists.

## Shapes
Panels use 22dp radii, controls 12–16dp, status indicators circular. No unrelated radius styles.

## Components
- **Operational dial:** simple stroked ring giving a glanceable ON AIR/STANDBY signal; static to conserve power.
- **Mode switch:** paired horizontal segments, high-contrast selection.
- **Game panel:** app identity and one switch on first line; tuning revealed only when opted in.
- **Teach dialog:** screenshot preview in memory, exact mapped pointer selection, restriction to corner touch targets, explicit Save/Discard.

## Do's and Don'ts
Do: purposeful hierarchy; obvious safety state; accessible action descriptions; reversible choices; tell users when reward status is unknown. Don't: generic purple hero gradients; decorative cards inside cards; fake dashboard numbers; claims of completed rewards; perpetual animations; silent actions outside selected games.
