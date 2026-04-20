# Jr Admin Runbook: Residency

## Purpose

This runbook explains how Jr admins should safely operate the Residency system.

Use this document when you need to:

- inspect a unit
- help a player with a rental issue
- assign or end a lease
- repossess a unit
- check escrow status
- define a new rentable unit
- add a linked subregion
- reload Residency after config changes

This page is operational. It is not the player guide and it is not the policy source of truth.

## What Residency covers

Residency handles rentable city properties, including:

- residential units
- storefronts
- mixed-use units

Storefronts are part of Residency. They are not a separate system.

## What Jr admins are allowed to do

Jr admins may usually do the following:

- inspect unit state
- reload Residency after approved config edits
- manually assign a lease
- terminate a lease when clearly appropriate
- repossess a unit into escrow when policy says to do so
- check escrow records
- grant a manager role to a player
- use the selection wand
- create a new unit after approval
- add a linked subregion after approval

## What Jr admins should escalate

Escalate to a higher admin when:

- there is a dispute over who should own or rent a unit
- repossession is contested
- escrow contents or compensation become disputed
- a player claims money was taken incorrectly
- approval-required units need exception handling
- district, building, pricing, or policy definitions need major changes
- a config mistake could affect many units
- overlapping regions or invalid world references appear
- staff are unsure whether to terminate or repossess
- a unit is in a broken or contradictory state

## Command quick reference

### Inspection and reload

`/resadmin info <unitId>`  
Show internal unit and tenancy state.

`/resadmin escrow <unitId>`  
Show escrow status if present.

`/resadmin reload`  
Reload Residency configs and runtime state.

### Lease operations

`/resadmin assign <unitId> <player>`  
Assign a lease to a player.

`/resadmin terminate <unitId>`  
Terminate a lease and mark the unit as repossessed.

`/resadmin repossess <unitId>`  
Move the unit into escrow-open state and create an escrow record.

`/resadmin addmanager <unitId> <player>`  
Add a manager to a unit.

### Unit creation and editing

`/resadmin wand`  
Get the Residency selection wand.

`/resadmin selection`  
View the current selection.

`/resadmin clearselection`  
Clear the current selection.

`/resadmin createunit <id> <district> <mode> <type> <pricingProfile> <policyProfile> [building]`  
Create a new unit from the current selection.

`/resadmin addsubregion <unitId> <name>`  
Add a linked subregion to an existing unit.

## Normal operating workflow

When a player asks for help, use this order:

1. inspect the unit
2. determine whether the issue is informational, billing-related, access-related, or administrative
3. take the smallest safe action
4. verify the result
5. escalate if the case is disputed or unclear

Do not start with destructive commands.

## Runbook: inspect a unit

Use this when:

- a player says a unit is broken
- a player says they cannot access a unit
- a player says a unit is not rentable
- you need to check current status before taking action

### Steps

1. Run:
   ` /resadmin info <unitId> `

2. Check:
   - district
   - building
   - lease state
   - rent state
   - tenant

3. If needed, also run:
   ` /resadmin escrow <unitId> `

### What to look for

- **Unknown unit**: the unit ID is wrong or the unit does not exist
- **Lease state active**: currently rented
- **Lease state listed or vacant**: available or expected to be available
- **Lease state repossessed**: not a normal player-access state
- **Lease state escrow open**: admin-controlled recovery state
- **Tenant none**: no current tenant is assigned

### Safe next actions

- If the problem is only confusion, explain the current state to the player
- If the player should be renting it but is not, escalate before assigning unless the case is clear
- If escrow or repossession is involved, be careful and follow policy

## Runbook: manually assign a lease

Use this only when:

- the assignment is clearly approved
- there is no dispute
- the target player is confirmed
- the unit is the correct one

### Command

`/resadmin assign <unitId> <player>`

### Before you run it

Check:

- correct unit ID
- correct player name
- no current dispute
- the unit should actually be assigned
- the player understands the assignment

### After you run it

1. Confirm success message
2. Re-run:
   ` /resadmin info <unitId> `
3. Check that the tenant is now set correctly
4. Ask the player to verify access if needed

### Do not use assign when

- the unit is under dispute
- repossession/escrow handling is still unresolved
- approval is uncertain
- you are trying to “fix” an unclear billing problem without reviewing the case

## Runbook: terminate a lease

Use termination when the lease should be ended and the case does **not** need escrow handling.

### Command

`/resadmin terminate <unitId>`

### What it does

- ends the lease
- clears the tenant
- clears managers
- marks the unit as repossessed state

### Before you run it

Check:

- you have the correct unit
- the lease should actually end
- no one expects escrow handling instead
- the player issue is not just access confusion or temporary arrears

### After you run it

1. Re-run:
   ` /resadmin info <unitId> `
2. Confirm tenant is cleared
3. Confirm lease state changed
4. Note the action in staff records if your team uses them

### Do not use terminate when

- items, compensation, or property recovery are involved
- you actually need escrow
- the case is disputed
- you only mean to inspect or pause action

## Runbook: repossess a unit into escrow

Use repossession when the unit should move into admin-controlled escrow handling.

### Command

`/resadmin repossess <unitId>`

### What it does

- sets the unit to escrow-open state
- ends the lease
- clears tenant and managers
- creates an escrow record

### Before you run it

Check:

- repossession is actually justified
- you have the correct unit
- the case should go to escrow, not simple termination
- the player situation has been reviewed properly

### After you run it

1. Run:
   ` /resadmin info <unitId> `
2. Run:
   ` /resadmin escrow <unitId> `
3. Confirm:
   - lease state is escrow-related
   - tenant is cleared
   - escrow record exists

### Escalate if

- the player disputes the repossession
- there is uncertainty about stored items or recovery
- money, compensation, or exceptions are involved

## Runbook: check escrow status

Use this when:

- a player asks what happened after repossession
- staff need to verify whether a unit is in escrow
- you need to confirm whether manual repossession already created a record

### Command

`/resadmin escrow <unitId>`

### What to expect

- an escrow status and reason
- or a message that no escrow record exists

### Next steps

- if no escrow exists but you expected one, escalate
- if escrow exists, use that information to route the case properly
- do not promise player outcomes unless policy is clear

## Runbook: add a manager

Use this when a manager role has been approved for a unit.

### Command

`/resadmin addmanager <unitId> <player>`

### Before you run it

Check:

- correct unit
- correct player
- approval is clear
- the tenant or case record supports this change

### After you run it

1. Confirm success
2. Re-run:
   ` /resadmin info <unitId> `
3. Ask the player to test access if needed

### Important note

Managers are not the same as tenants. Do not describe them as owners. Manager permissions may still be limited compared with full tenancy access.

## Runbook: safe reload

Use reload when:

- approved config files were changed
- a new unit was added
- a linked subregion was added
- a residency config correction was made
- staff need the current Residency data reloaded

### Command

`/resadmin reload`

### Safe reload checklist

Before reload:

- confirm edits are finished
- confirm no one else is mid-edit
- confirm you are reloading the right feature
- make sure the config change was approved

Run:

`/resadmin reload`

After reload:

1. watch for obvious errors
2. inspect the affected unit:
   ` /resadmin info <unitId> `
3. verify the new or changed unit exists
4. verify expected state
5. if the reload introduced broken behavior, escalate immediately

### Do not reload casually

Avoid unnecessary reloads while troubleshooting if you do not know what changed.

## Runbook: use the selection wand

Use the wand for creating units or linked subregions.

### Get the wand

`/resadmin wand`

### Set positions

- left-click a block for position 1
- right-click a block for position 2

### Check your current selection

`/resadmin selection`

### Clear selection

`/resadmin clearselection`

### Good practice

- verify both positions before creating anything
- make selections carefully and deliberately
- do not assume your old selection is still correct
- clear your selection if you are switching tasks

## Runbook: create a new unit

Use this only after approval to define a new rentable property.

### Command

`/resadmin createunit <id> <district> <mode> <type> <pricingProfile> <policyProfile> [building]`

### Before you create the unit

Confirm:

- the unit has been approved
- the ID naming is correct
- the district exists
- the building is correct if applicable
- the pricing profile is correct
- the policy profile is correct
- your wand selection is complete and accurate

### Suggested procedure

1. Get the wand:
   ` /resadmin wand `
2. Select pos1 and pos2
3. Check:
   ` /resadmin selection `
4. Run the create command
5. The system saves the unit and reloads Residency
6. Inspect the unit:
   ` /resadmin info <unitId> `

### After creation

Verify:

- the unit exists
- the district is correct
- the building is correct
- the unit is rentable as intended
- the region does not obviously conflict with nearby units

### Escalate if

- the district is unknown
- mode or type is unclear
- the new unit overlaps another property
- reload errors appear
- you are unsure which pricing or policy profile to use

## Runbook: add a linked subregion

Use this to attach an additional managed area to an existing unit.

Examples might include:

- a connected storage area
- an attached balcony or side room
- a defined secondary space that should count as part of the unit

### Command

`/resadmin addsubregion <unitId> <name>`

### Procedure

1. Get the wand if needed:
   ` /resadmin wand `
2. Select the two corners
3. Check:
   ` /resadmin selection `
4. Run:
   ` /resadmin addsubregion <unitId> <name> `
5. The system saves and reloads Residency

### After adding the subregion

- verify the unit still behaves correctly
- verify access in the linked area if relevant
- escalate if the new subregion seems to interfere with another unit

## Common cases and what to do

### A player says “I cannot access my unit”
Check:

1. ` /resadmin info <unitId> `
2. whether they are actually the tenant
3. whether the unit is repossessed or in escrow
4. whether they are a manager rather than a tenant
5. whether the issue is actually a permissions misunderstanding

If the state looks wrong or disputed, escalate.

### A player says “I was removed from my rental”
Check:

1. ` /resadmin info <unitId> `
2. ` /resadmin escrow <unitId> `

Look for:

- repossession
- escrow
- cleared tenant
- unusual state transitions

Do not promise reversal. Escalate if disputed.

### A player says “My storefront is gone”
Treat storefronts as Residency units.

Check:

- whether the unit still exists
- whether the unit is listed, active, repossessed, or escrowed
- whether the player is still the tenant
- whether a config edit or reload recently changed something

### A player says “I paid but it still looks wrong”
Check:

- current unit info
- whether the player is the actual tenant
- whether the wrong unit ID was used
- whether the case involves arrears, restriction, or repossession

If money handling looks inconsistent, escalate.

### A new unit was added but does not behave correctly
Check:

- selection accuracy
- district and building values
- pricing and policy values
- result after reload

If the issue may involve overlap or invalid config relationships, escalate.

## What not to do

Do not:

- assign units casually without approval
- use terminate when escrow is needed
- use repossess just because a player is inconvenient
- promise compensation or item recovery outcomes
- edit live property definitions without approval
- create units from uncertain or sloppy selections
- guess pricing or policy profile values
- treat managers as full tenants in all cases
- use destructive commands first when inspection would answer the question
- ignore reload or validation issues

## Safe decision guide

### Use `info` first when:
- you are not sure what happened
- a player reports something odd
- you are about to take action on a unit

### Use `assign` when:
- the unit should be leased to a specific player
- approval is clear
- there is no dispute

### Use `terminate` when:
- the lease should end
- escrow is not needed
- the case is clear and approved

### Use `repossess` when:
- the unit should move into escrow handling
- policy supports that action
- the case has been reviewed properly

### Use `reload` when:
- approved config changes were made
- unit definitions were added or changed
- you need Residency to re-read its current configuration

## Handoff notes for escalation

When escalating, include:

- unit ID
- player name
- what happened
- which commands you already ran
- current lease state
- current rent state if known
- whether escrow exists
- whether you already took any action
- whether the case involves money, access, or disputed ownership

## Summary

Jr admins should use Residency tools carefully and in a small-step order:

1. inspect first
2. act only when the case is clear
3. verify after every change
4. escalate whenever money, disputes, escrow, or unclear policy are involved

Residency is a live operations feature. Treat every command as a real change to player housing or storefront ownership.