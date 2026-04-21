# Smart Light Systems

Smart Light Systems lets players interact with configured light installations around the server.

At the simplest level, a Smart Light setup works like this:

- a specific button is assigned as a switch
- that switch is linked to one or more light blocks
- when the button is pressed, the linked lights turn on or off

This system is used for decorative city lighting, immersive building infrastructure, and interactive public spaces.

---

## What it does

Smart Light Systems supports two main kinds of behavior:

### 1. Manual light switching
A configured button can toggle linked lights when pressed.

Depending on the build, this may work by:

- swapping a lit block with an inert-looking block
- toggling the lit state of supported light blocks

### 2. Automatic day/night behavior
Some configured lights are also affected by the server’s automatic celestial lighting behavior.

In general:

- lights are turned off around Minecraft midnight
- lights are turned back on around dawn

This helps keep city lighting feeling dynamic and alive.

---

## How to use it

### Toggle a light installation
If a build has a configured Smart Light switch:

1. walk to the assigned button
2. press it
3. the linked lights should toggle

If nothing happens, the button may not be part of a configured Smart Light setup, or the linked lights may need staff review.

---

## Player commands

## `/scanlights`
Scans the light region you are currently standing in and lists supported light blocks found there.

Use this when you want to inspect a region’s configured light blocks.

### Notes
- you must be inside exactly one configured light region
- this is mainly useful for builds or maintenance checks

---

## `/listlightregions`
Lists all configured Smart Light regions.

Use this to see what regions exist.

---

## `/checklightregion`
Checks whether you are currently inside a configured light region.

Use this if you are unsure whether a building or area is part of the Smart Light system.

---

## `/lightinfo`
Looks at the light block you are targeting and shows:

- its location
- which light region it belongs to
- which configured button switches control it

Use this when troubleshooting a specific light block.

### Notes
- you must be looking directly at a supported light block
- the command checks only a short distance in front of you

---

## Who can use what

Players are generally intended to use:

- `/scanlights`
- `/listlightregions`
- `/checklightregion`
- `/lightinfo`

Some Smart Light commands are staff/admin-oriented, such as configuration reloads and exports.

If you do not have permission for a command, the server will deny it.

---

## Supported light behavior

Smart Light Systems can work with configured light block types, including mapped on/off block pairs and certain lit-state blocks.

Examples of mapped behavior include builds that switch between:

- Sea Lantern and White Concrete
- Glowstone and Cobblestone
- Jack o’ Lantern and Carved Pumpkin
- Shroomlight and Orange Concrete

Some other supported light blocks can be toggled by changing their lit state instead of swapping the block itself.

Because the system is config-driven, not every light in the server is automatically part of Smart Light Systems.

---

## Tips

- If a button does nothing, make sure it is the actual designated Smart Light switch.
- If you are testing a light area, use `/checklightregion` first.
- If you need to inspect a specific light, use `/lightinfo` while looking directly at it.
- If a region seems incomplete or wrong, use `/scanlights` to inspect what the system currently sees.
- Automatic day/night behavior may affect some configured lights even if no one presses a switch.

---

## Common issues

### “The button does nothing.”
Possible reasons:

- the button is not a configured Smart Light switch
- the linked lights were configured incorrectly
- the installation needs staff maintenance

### “`/scanlights` says I’m not in any light region.”
That means your current position is not inside a configured Smart Light region.

Move into the intended area and try again.

### “`/scanlights` says I’m in multiple regions.”
That means the area overlaps more than one light region.

This needs staff review.

### “`/lightinfo` says I’m not looking at a valid light block.”
That usually means either:

- you are not looking directly at the block
- the block is too far away
- the block is not one of the supported Smart Light block types

### “The lights did not all change automatically at night or dawn.”
Some automatic changes may not happen if the relevant area is not currently loaded.

---

## FAQ

### Are all lights on the server part of Smart Light Systems?
No. Only configured lights and configured switches are part of this feature.

### Can one button control multiple lights?
Yes. A single configured switch can control multiple linked light locations.

### Can I use Smart Light Systems for my build?
That depends on how your server staff manages custom feature setup. Ask staff if your build is meant to be added to the system.

### Why does a light look like a different block when turned off?
Some Smart Light setups work by swapping a lit block with a non-light version to simulate lights being off.

### Does this feature use real-world time?
No. The automatic on/off cycle described here follows Minecraft world time.

---

## What Smart Light Systems is not

Smart Light Systems is not:

- the beacon color-show system
- a general redstone replacement
- a claim, economy, or ownership feature
- a guarantee that every decorative light can be toggled

It is a specific configured system for switchable and inspectable light installations.

---

## When to contact staff

Contact staff if:

- a public switch clearly should work but does not
- a light installation is linked incorrectly
- a region appears missing or overlapping
- an important city light setup is broken
- you believe a configured setup was changed incorrectly