## Why

Children can connect the picture they just finished with its spoken name. Learning already provides spoken feedback when a child finishes tracing.

## What Changes

- Bundle an English recording for each of the 100 coloring pictures.
- Say the picture's name once when the child finishes in Assisted or Follow Along with sound enabled.
- Stop speech on departure from the completed picture and follow the existing sound setting.

## Capabilities

### Modified Capabilities

- `coloring` gains spoken completion feedback for guided coloring.

## Impact

The coloring completion event in `PlayActivity` starts a bundled recording through the existing speech player. The app uses the same voice as the learning lessons.
