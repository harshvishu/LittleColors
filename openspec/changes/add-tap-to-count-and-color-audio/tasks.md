## 1. Bundled Audio Assets

- [x] 1.1 Generate `app/src/main/assets/learning/speech/count_1.wav` through `count_10.wav` (mono 22050 Hz 16-bit PCM WAV)
- [x] 1.2 Generate `app/src/main/assets/coloring/colors/<slug>.wav` for all 62 unique swatch names in `pages.json` and update `CREDITS.txt`

## 2. Interactive Tap-to-Count (123 Reveal)

- [x] 2.1 Update `LessonIllustration.kt` to hit-test object slots, track sequential counts, and animate a tap bounce when `onItemCounted` is provided
- [x] 2.2 Wire `onItemCounted` in `PlayActivity.kt` for `Book.NUMBERS` reveals to play `learning/speech/count_<n>.wav` via `LessonNarration` while preserving tap-to-trace-again outside objects

## 3. Spoken Color Names on Swatch Tap

- [x] 3.1 Update `PlayActivity.kt` to play `coloring/colors/<slug>.wav` when a swatch is tapped in `openPicker()` or the Freehand footer (respecting `sounds.enabled` and lifecycle stop)

## 4. Verification

- [x] 4.1 Validate OpenSpec change strictly and run `./gradlew :app:assembleDebug :app:lintDebug`
