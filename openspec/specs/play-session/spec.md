# Play session

## Purpose

Let parents configure and manage a toddler's offline play session, with settings kept on the device.

## Requirements

### Requirement: Optional Android screen pinning

The app SHALL request Android screen pinning when starting play and offer unpinned play after a pinning attempt leaves the app unpinned.

#### Scenario: Select how to start play

- **WHEN** Android confirms pinning or the parent selects Play without pinning
- **THEN** the app opens the selected picture or lesson

### Requirement: Hold controls

The play screen SHALL require a two-second hold on Home to return to the Parent menu and a five-second hold on × to close the app. Closing the app SHALL release screen pinning when active.

#### Scenario: Briefly touch a hold control

- **WHEN** the user releases Home or × before its hold completes
- **THEN** play stays open

### Requirement: Local settings and offline play

The app SHALL provide sound and music toggles and store preferences and most recent selections on the device. Drawings and trace progress SHALL live in memory. Play SHALL work offline using bundled assets, without accounts, ads, analytics, or tracking.

#### Scenario: Relaunch offline

- **WHEN** the app is relaunched without an Internet connection
- **THEN** it restores the saved settings and selections and allows the parent to resume play from the Parent menu
