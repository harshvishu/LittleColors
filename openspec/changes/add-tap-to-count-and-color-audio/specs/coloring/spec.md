## ADDED Requirements

### Requirement: Spoken color names on swatch selection

The app SHALL bundle an offline English recording for every swatch name used in the coloring catalog and play it when the child selects a swatch with sound enabled.

#### Scenario: Pick a color in Assisted or Follow Along

- **WHEN** the child taps a swatch in the color picker with sound enabled
- **THEN** the app applies the color and speaks that swatch's name offline

#### Scenario: Pick a color in Freehand

- **WHEN** the child taps a swatch in the Freehand footer with sound enabled
- **THEN** the app selects that swatch and speaks its name offline

#### Scenario: Pick a color with sound off

- **WHEN** the child taps a swatch in any coloring mode with sound disabled
- **THEN** the app applies or selects the color without playing spoken audio
