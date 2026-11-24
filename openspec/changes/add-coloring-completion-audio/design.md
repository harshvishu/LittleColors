## Context

`DrawingBoard` emits `Cue.FINISH` once when the last region is filled. `PlayActivity` already handles the chime and confetti and uses `LessonNarration` for bundled speech and music ducking.

## Goals

- Give every guided coloring picture an offline spoken label on completion.
- Keep the app's sound setting and learning narration behavior.

## Decisions

- Generate short PCM WAV recordings with the existing Piper LJSpeech voice. Use a filename derived from the names in the coloring catalog, so reordering the catalog keeps pictures matched to recordings.
- Queue playback from `Cue.FINISH` after the main part of the chime. Reuse `LessonNarration` for playback and music ducking. Clear pending and active speech when leaving the picture or pausing the app.
- Use the existing guided completion event. Freehand remains open-ended, and learning reveals keep their existing speech, including each number and its counting example.

## Risks / Trade-offs

- The recording set adds to the APK size. Verify that each picture maps to exactly one packaged recording and match clip loudness to the existing lesson narration.
