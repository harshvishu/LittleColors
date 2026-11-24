# Coloring

## Purpose

Give toddlers an offline picture catalog with touch-and-hold coloring, guided tracing, and free drawing.

## Requirements

### Requirement: Picture catalog and palettes

The app SHALL provide 100 ordered coloring pictures with six named swatches per picture.

#### Scenario: Select a picture

- **WHEN** a parent chooses a picture in the Coloring section and starts play
- **THEN** the app opens that picture in the selected drawing mode and uses its palette for color selection

### Requirement: Assisted and Follow Along coloring

The app SHALL trace the outline before offering colors to fill one region at a time. Assisted SHALL advance while a finger is held on the canvas. Follow Along SHALL advance in response to movement near the guide.

#### Scenario: Choose a region color

- **WHEN** the outline or a region is complete and an unfilled region remains
- **THEN** the app opens the picture's color picker, indicates the suggested color, and accepts any of the six swatches

#### Scenario: Finish a picture

- **WHEN** the last region is filled
- **THEN** the app displays the completed picture and offers the next picture, returning to the first picture after the last one

### Requirement: Freehand coloring

The app SHALL show the picture outline and provide its six swatches and an eraser for freehand drawing.

#### Scenario: Draw and erase

- **WHEN** the child draws with a selected swatch and then drags the eraser over those strokes
- **THEN** the app applies the selected color and clears the ink under the eraser while preserving the picture outline
