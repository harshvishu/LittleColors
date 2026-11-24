## ADDED Requirements

### Requirement: Spoken picture completion

The app SHALL bundle an English recording of the name of every coloring picture. Upon finishing a picture in Assisted or Follow Along, it SHALL play the corresponding recording once when sound is enabled.

#### Scenario: Hear the finished picture offline

- **WHEN** the child colors the final region with sound enabled and the device has no internet connection
- **THEN** the app says that picture's name after the completion cue

#### Scenario: Finish with sound off

- **WHEN** the child finishes a picture with sound disabled
- **THEN** the app shows the completed picture without playing a spoken label

#### Scenario: Leave the completed picture

- **WHEN** the child advances, returns to the Parent menu, or leaves the app before the recording ends
- **THEN** the app cancels any pending or active speech for that picture
