# Beacon Light Shows

## What this is
Beacon Light Shows are decorative animated beacons on the server. They automatically cycle through a predefined sequence of colored glass to create a visual light show.

This is an environment feature, not a player progression system. Players are meant to enjoy and recognize the displays, not configure them directly.

## What players can do
Players can:

- view beacon light shows in the world
- use them as landmarks, ambience, or part of the city experience
- report a broken or stuck beacon show to staff

Players do **not** manage, pause, resume, or retime beacon shows themselves unless your server later grants special permissions.

## How it works
Each beacon show is tied to a configured beacon location.

The system updates the five glass blocks above the beacon on a repeating timer. Over time, the glass changes through a configured color cycle to create an animated transition effect.

In practice, this means:

- the beacon show runs automatically
- colors change over time
- staff can speed up, slow down, pause, or resume the sequence
- the show starts automatically when the plugin starts

## Commands
There are currently **no player-facing beacon show commands confirmed** from the provided code.

The feature does have beacon commands in the plugin, but the supplied implementation indicates those are operational controls for staff:

- `/pausebeacons`
- `/resumebeacons`
- `/reloadsunwayconfig`
- `/setbeaconticks`

Unless your permissions setup says otherwise, players should treat Beacon Light Shows as an automatic world feature rather than a command-based feature.

## Rules and expectations
- Do not break, obstruct, or alter beacon show builds unless explicitly allowed.
- Do not assume a beacon show is player-owned.
- If a show stops changing color, report it to staff instead of trying to fix it yourself.
- Beacon Light Shows are decorative and may be changed by staff as part of world maintenance, events, or aesthetic updates.

## Tips
- Use beacon shows as wayfinding markers in important public areas.
- If you notice a beacon frozen on one color, missing glass, or showing the wrong blocks, note the location before reporting it.
- If multiple beacon shows appear broken after a restart or update, include that in the report so staff know it may be a wider config issue.

## FAQ

### Do beacon light shows do anything besides look nice?
Based on the current feature design, they are decorative visual effects.

### Can I make my own beacon light show?
Not through any player-facing system shown in the current implementation. Beacon locations are configured by staff.

### Why did the beacon stop changing color?
Possible reasons include:
- staff paused the beacon system
- the server restarted
- the configuration for that beacon is wrong
- the show is currently being adjusted by staff

### Can players change the speed or colors?
Not through any confirmed player-facing commands in the provided code.

### Can beacon shows be temporarily turned off?
Yes, staff can pause and resume the system.

### What should I report to staff?
Report:
- the exact location
- whether the beacon is missing glass
- whether it is stuck on one color
- whether multiple beacon shows seem broken
- whether the issue started after a restart or event

## Known limitations
- No player customization tools are currently documented.
- No player command workflow is currently documented.
- Beacon shows appear to be centrally configured by staff.
- Live config reload behavior may not fully apply every change immediately, so staff may need to do additional operational steps when maintaining the feature.