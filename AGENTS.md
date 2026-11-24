# Little Colors repository instructions

## Working here

- Keep changes focused. Inspect the owning code and specs first, reuse existing behavior, and preserve persisted preferences and asset compatibility. For non-trivial work, state assumptions and outline the implementation and checks.
- Use OpenSpec for app changes. Review `openspec/specs/` and `openspec list`. Use `$openspec-explore` when the behavior is still unclear and `$openspec-propose` to draft the proposal, spec deltas, design, and tasks. Keep each change under `openspec/changes/<change-id>/`.
- Use `$openspec-apply-change` to implement and verify the tasks. Validate the change with `openspec validate <change-id> --type change --strict`. When the change is ready to close, use `$openspec-archive-change` to merge the deltas into the baseline specs and archive the change. Run `openspec validate --specs --strict` after syncing.
- Keep `openspec/config.yaml`, current specs, changes, and archives in Git. The editor integrations are generated locally. Regenerate the selected core integrations with `openspec init --tools codex,antigravity,github-copilot --profile core`.
- Build or refresh the local graph with `gitnexus analyze --skip-agents-md` and check it with `gitnexus status`. Use GitNexus to trace callers and assess impact when useful, then verify against the source. `.gitnexus/` is local. The flag preserves the project-maintained instructions in this file.

## Architecture

Little Colors is a single native Android application module with package and application ID `family.littlecolors`. The build uses Kotlin, JDK 17, minSdk 28, and compile/target SDK 36. UI is built with Android Views in code. Artwork, drawing, and overlays use Canvas.

| Area | Where to work |
| --- | --- |
| App entry and session | `PlayActivity.kt` owns the Parent menu, section and mode selection, play layouts, color picker, screen pinning, hold controls, settings, and lifecycle. The manifest declares the activity and configuration changes it handles. |
| Drawing and guidance | `DrawingBoard.kt` handles touch, guide paths, tracing, region filling, freehand strokes, and erasing. Coloring and learning use separate instances of this View. `FingerGuide.kt` shows the gesture or points to a control, and `Confetti.kt` provides completion feedback. |
| Coloring catalog | `ColoringPages.kt` defines the ordered picture list. `Pictures.kt` defines the model. `VectorArtwork.kt` reads paths, reference colors, six named swatches per page, and marks from `app/src/main/resources/artwork/pages.json`. It also renders the illustrations. |
| Learning catalog | `LearningBook.kt` reads `app/src/main/assets/learning/lessons.json` and `illustrations.json`, reuses coloring illustrations by name, and provides ordered trace paths and narration filenames. `LessonIllustration.kt` draws the reveal and repeats the illustration for counting. |
| Audio | `PlaySounds.kt` handles sound effects and the pencil loop. `LessonNarration.kt` plays bundled intro and reveal WAV files. `PlayActivity.kt` controls the lullaby and lowers its volume during narration. |

Kotlin source lives in `app/src/main/java/family/littlecolors/`. Coloring JSON is loaded as a classpath resource and learning JSON and narration are loaded through Android assets. Vector data uses a 320 × 300 board coordinate space and absolute `M`, `L`, `Q`, `C`, and `Z` commands.

## Decisions to preserve

- **Coloring** has 100 ordered pages with picture-specific palettes. Assisted advances while the child holds a finger down. Follow Along advances with movement near the guide. Both trace the outline first and then fill regions one at a time. Freehand starts with the outline visible and offers swatches and an eraser that clears drawn ink.
- **Learning** covers uppercase A–Z and numbers 1–10, with Assisted and Follow Along. A completed trace flips to an illustration and caption or a group of objects to count. The child can replay narration, tap the reveal to trace again, or advance. The last lesson in each book is remembered independently.
- **Parent controls** use Android screen pinning with an option to play unpinned. A two-second Home hold returns to the Parent menu. A five-second × hold closes the app and releases pinning. Keep these actions and the sound and music settings accessible on phone and tablet layouts.
- **Privacy and persistence** are local. The app has no accounts, ads, analytics, tracking, or Internet permission. `SharedPreferences("play")` stores sound, music, the coloring mode and page, the active section, and learning settings. Drawings and trace progress stay in memory. Saved selections are indexes, so changing the order or removing catalog entries can change what an existing user sees. Preserve the legacy `free` preference fallback when editing mode loading.
- **Assets and distribution** use original artwork and attributed Twemoji adaptations (CC BY 4.0), with bundled Piper/LJSpeech narration. Update the relevant `CREDITS.txt` and `NOTICE` when provenance changes. App source and original assets use PolyForm Noncommercial 1.0.0. Keep the application ID and distribution signing continuity for F-Droid and Google Play updates. See [Play Store release and signing instructions](docs/play-store/README.md).

## Verification

- Use JDK 17 and Android SDK 36. Run `./gradlew :app:assembleDebug :app:lintDebug` for app changes.
- Try the affected flow on a device or emulator. For drawing changes check the affected modes, lift and cancel behavior, and completion. For UI changes cover phone and tablet layouts and rotation where applicable. For session changes cover pinned and unpinned play, hold controls, and audio across pause and resume.
