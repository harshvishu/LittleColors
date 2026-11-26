## Context

`PlayActivity` already owns `LessonNarration` for bundled speech playback with automatic lullaby ducking, and `LessonIllustration` draws the grid of objects on a completed lesson reveal. `pages.json` already defines six named swatches per coloring page (62 unique color names total).

## Goals

- Let toddlers tap individual objects on a 123 reveal to count aloud from 1 to the lesson's quantity with tactile visual feedback, while preserving tap-to-trace-again on the reveal card.
- Speak the selected swatch's color name whenever a child picks a swatch in Assisted, Follow Along, or Freehand mode.
- Keep all audio offline, bundled in the APK, respectful of the sound toggle, and cleanly cancelled when leaving or pausing play.

## Decisions

- **Sequential tap-to-count state in `LessonIllustration`**: Store per-slot assigned counts (`IntArray(lesson.count)`), an animation progress array for a subtle pop/scale bounce, and a running `counted` total. When a child taps an uncounted object slot, assign it the next number (`1..lesson.count`), animate a short bounce, and invoke `onItemCounted(number)` so `PlayActivity` plays `learning/speech/count_<number>.wav`. Tapping an already-counted object repeats its assigned number and bounce. Once all objects are counted, tapping another object resets and starts from `1` again.
- **Preserving flip-back**: `LessonIllustration` only handles `ACTION_UP` touches that land inside a drawn object's bounding box; touches outside object slots (or on the caption) bubble up to the reveal card's existing click listener to flip back and restart tracing. ABC lessons (`Book.ABC`) keep `onItemCounted = null` so tapping anywhere on the ABC reveal continues to flip back immediately.
- **Color-name audio lookup**: Derive the asset filename from the swatch name (`name.lowercase(Locale.ROOT).replace(' ', '_')`) under `coloring/colors/<slug>.wav`. Play it via `LessonNarration` whenever `sounds.enabled && playing && resumed` on swatch selection in both `openPicker()` and the Freehand footer.
- **Audio generation**: Synthesize mono 22050 Hz 16-bit PCM WAV files using macOS `say` + `afconvert` (matching the existing WAV format in `assets/learning/speech/`), and document the voice/tooling in `CREDITS.txt`.

## Risks / Trade-offs

- **APK size**: 72 short WAV files (10 counting + 62 colors) add modest asset size (~1–2 MB). Using 22050 Hz mono 16-bit PCM keeps them compact and consistent with existing speech assets.
- **Voice consistency**: Because Piper is not installed in this environment, the new clips use macOS `say` (`Samantha` or default system voice) converted to 22050 Hz mono PCM WAV; they can be regenerated with Piper LJSpeech later without any code changes because filenames are deterministic.
