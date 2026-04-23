# Residency

Residency is the server’s property rental system for homes and storefront spaces in admin-managed city areas. It lets you browse available units, rent them, pay rent, manage guest access, and leave a unit when you are done.

Storefronts are part of Residency. They are not a separate system.

## What you can do

With Residency, you can:

- browse available homes and rentable spaces
- view unit details before renting
- rent a unit if it is available and you can afford it
- pay rent on your current unit
- see which units you rent or manage
- give temporary guest access to other players
- leave a unit you no longer want

If you are looking specifically for commercial spaces, use the storefront listing command.

## Commands

### Browse and inspect units

`/residency list`  
Shows available rentable units.

`/residency view <unitId>`  
Shows details about a unit, including district, type, rent, deposit, and whether approval is required.

`/storefront list`  
Shows available storefronts and other rentable commercial or mixed-use spaces.

### Rent and manage units

`/residency rent <unitId>`  
Attempts to rent a unit.

`/residency pay <unitId>`  
Pays rent for a unit you currently rent.

`/residency myunits`  
Shows the units you currently rent or manage.

`/residency guest <unitId> <player> [hours]`  
Grants temporary guest access to another player. If no hour value is given, the default is 24 hours.

`/residency leave <unitId>`  
Starts the process to leave a unit.

`/residency leave <unitId> confirm`  
Confirms that you want to leave the unit.

`/residency help`  
Shows the command list.

## How renting works

When you rent a unit, the system checks whether:

- the unit exists
- the unit is available
- pricing is set correctly
- you have enough money

When a rental starts, you are charged the upfront cost. In the current system, that means:

- the deposit
- the first rent payment

If the rent succeeds, the unit becomes your active rental.

## How to find a good unit

Start with:

`/residency list`

Then inspect one you like with:

`/residency view <unitId>`

That will help you compare:

- rent
- deposit
- unit type
- district
- whether the unit requires approval

If you only want a shop or business location, use:

`/storefront list`

## Your access and your role

### Tenant

If you are the tenant, you are the main renter of the unit.

As a tenant, you can:

- occupy the unit
- pay rent
- leave the unit
- grant guest access
- appear as the primary renter in the system

### Manager

Some units may have a manager role assigned by staff.

Managers can help manage the unit, but they are not the main tenant. Some permissions may be more limited than full tenant permissions.

### Guest

Guests have temporary access granted by a tenant, manager, or staff.

Guest access is time-limited.

## Guest access

Use:

`/residency guest <unitId> <player> [hours]`

Example:

`/residency guest apt-12 Alex 48`

That grants Alex guest access to `apt-12` for 48 hours.

If you do not provide a duration, the system uses 24 hours by default.

Only people with authority over that unit can grant guest access.

## Leaving a unit

To stop renting a unit:

`/residency leave <unitId>`

The system will warn you and ask you to confirm. Then run:

`/residency leave <unitId> confirm`

Leaving a unit will end your lease and revoke your access.

Do not use the confirm step unless you are sure.

## Paying rent

Use:

`/residency pay <unitId>`

If the payment succeeds, your rent status is updated and your next due date moves forward.

If you do not have enough funds, the payment will fail.

## Late rent and missed payments

If rent comes due and cannot be collected, the unit may move through several states over time, such as:

- grace period
- arrears restriction
- repossession or escrow

What this means for players:

- do not ignore overdue rent
- pay as soon as possible if you fall behind
- if access changes or restrictions appear, contact staff if you believe something is wrong

## Storefronts

Storefronts are part of the Residency system.

Use storefront listings if you want:

- a commercial shop
- a mixed-use rental space
- a business location in a managed city area

The storefront command is mainly a filtered listing view for business-suitable units.

## Tips

- Always inspect a unit with `/residency view` before renting it.
- Keep enough money available for rent when it comes due.
- Use `/residency myunits` if you forget which unit IDs you currently have.
- Be careful when using `/residency leave`, because it is a real lease-ending action.
- Grant guest access only to players you trust.

## FAQ

### I tried to rent a unit and it failed.
Possible reasons include:

- the unit is no longer available
- you do not have enough money
- pricing is missing or invalid
- the unit has special approval handling

### How do I see my current rentals?
Use:

`/residency myunits`

### How do I find shop spaces?
Use:

`/storefront list`

### Can I let a friend into my place?
Yes, if you rent or manage the unit, use:

`/residency guest <unitId> <player> [hours]`

### How do I stop renting?
Use:

`/residency leave <unitId>`

Then confirm with:

`/residency leave <unitId> confirm`

### What if my rent is overdue?
Pay as soon as possible. If you wait too long, the unit may become restricted or be repossessed.

## Summary

Residency is the main system for renting homes and storefronts in managed city areas. Use it to browse units, rent them, stay current on payments, manage guest access, and leave when you are done.