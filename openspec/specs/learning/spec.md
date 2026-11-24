# Learning

## Purpose

Help toddlers practice tracing letters and numbers and connect the symbols to words and quantities.

## Requirements

### Requirement: Lesson books and trace modes

The app SHALL offer an ABC book for uppercase A–Z and a 123 book for numbers 1–10. Each SHALL support Assisted and Follow Along tracing using the lesson's stroke paths.

#### Scenario: Start a lesson

- **WHEN** a selected lesson opens
- **THEN** the app introduces the symbol, then shows its dotted trace and accepts input in the selected mode

#### Scenario: Return to a book

- **WHEN** a parent returns to a book after using the other book or relaunching the app
- **THEN** the app restores that book's most recently selected lesson

### Requirement: Lesson reveal and replay

The app SHALL flip the card after tracing completes. A letter SHALL reveal one illustration and a caption. A number SHALL reveal a matching quantity of objects and a counting caption.

#### Scenario: Practice again or advance

- **WHEN** a child taps the reveal or uses its advance button
- **THEN** tapping the card restarts the current trace, and advancing opens the next lesson or returns to the Parent menu at the end of the book

#### Scenario: Hear the lesson offline

- **WHEN** sound is enabled and the lesson opens, reveals, or the child taps replay narration
- **THEN** the app plays the bundled narration for the visible side of the card
