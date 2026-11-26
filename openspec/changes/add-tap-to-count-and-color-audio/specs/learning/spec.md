## ADDED Requirements

### Requirement: Interactive tap-to-count on number reveals

When a 123 number lesson reveals its objects, tapping an object SHALL count it aloud offline when sound is enabled and animate a visual bounce on that object, while tapping outside the objects or on the caption SHALL still restart tracing.

#### Scenario: Count objects by tapping

- **WHEN** the child taps uncounted objects on a 123 reveal with sound enabled
- **THEN** the app counts each tapped object aloud in sequence from 1 up to the lesson's number and animates that object

#### Scenario: Tap-to-count with sound off

- **WHEN** the child taps objects on a 123 reveal with sound disabled
- **THEN** the app animates the tapped object without playing counting speech

#### Scenario: Restart tracing from a number reveal

- **WHEN** the child taps the caption or background area outside the objects on a 123 reveal
- **THEN** the app flips back to the tracing side and restarts the current lesson
