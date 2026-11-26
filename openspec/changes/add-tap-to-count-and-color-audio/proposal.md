## Why

Toddlers using Little Colors currently hear a single caption on 123 number reveals and see silent color swatches while coloring. Adding interactive tap-to-count on 123 reveals and spoken color names when tapping swatches turns both existing activities into richer, playful vocabulary and counting practice without adding UI clutter or leaving offline play.

## What Changes

- **Interactive tap-to-count on 123 reveals**: Tapping objects on a completed number reveal counts them aloud one by one (`1` through the lesson's count) with a gentle visual bounce, while tapping outside the objects (or on the caption) still flips the card back to trace again.
- **Spoken color names on swatch taps**: Tapping any of the six color swatches in Assisted, Follow Along, or Freehand mode speaks that swatch's color name using bundled offline WAV recordings when sound is enabled.
- **Bundled offline audio**: Add 10 counting clips (`learning/speech/count_1.wav`–`count_10.wav`) and 62 color-name clips (`coloring/colors/<name>.wav`) matching the 62 unique swatch names in `pages.json`, and document their provenance in `CREDITS.txt`.

## Capabilities

### New Capabilities
- None

### Modified Capabilities
- `learning`: Add interactive tap-to-count behavior and bundled counting audio on 123 lesson reveals.
- `coloring`: Add bundled spoken color-name playback when a child selects a color swatch in any coloring mode.

## Impact

- `app/src/main/java/family/littlecolors/LessonIllustration.kt`: Hit-test tapped objects, track counted state, and animate a subtle tap bounce.
- `app/src/main/java/family/littlecolors/PlayActivity.kt`: Trigger counting audio on 123 object taps, trigger swatch color-name audio on swatch selection in picker and freehand footer, and cancel speech on pause/exit.
- `app/src/main/assets/learning/speech/` & `app/src/main/assets/coloring/colors/`: Bundled mono 22050 Hz 16-bit PCM WAV files and updated `CREDITS.txt`.
