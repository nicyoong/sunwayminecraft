# Coin Flip

## What it is

Coin Flip is a server feature that lets you gamble against the server using either money or items.

You choose **heads** or **tails**, place your wager, and the server flips a virtual coin. If your guess is correct, you win. If not, you lose your wager.

Coin Flip supports:

- **money wagers**
- **item wagers**
- optional **message muting** for coin flip result messages

---

## What you can use it for

Use Coin Flip when you want a simple chance-based gamble with either:

- in-game currency
- stackable items from your inventory

This is a server-versus-player system. You are not playing against another player.

---

## How it works

### Money mode

In money mode:

1. you choose heads or tails
2. you choose an amount of money to bet
3. the amount is taken from your balance
4. the server flips the coin
5. if you win, you get a payout
6. if you lose, your wager is gone

You must have enough money in your balance before starting the flip.

### Item mode

In item mode:

1. hold the item you want to bet in your main hand
2. choose heads or tails
3. choose how many of that item to bet
4. the server checks whether the item is allowed
5. the wagered items are removed
6. the server flips the coin
7. if you win, you receive item winnings
8. if you lose, the wagered items are gone

If your inventory is full when you win items, the extra items may be dropped at your location.

---

## Allowed item bets

You can only bet items that are suitable for stack-based wagers.

In general, Coin Flip is meant for normal stackable items.

### Usually allowed
Examples include common stackable materials such as:

- blocks
- ores
- resources
- food
- other normal stackable items

### Not allowed
You cannot bet non-stackable or protected item types, including things like:

- tools
- weapons
- armor
- elytra
- shields
- totems of undying
- compass
- clock
- bundle
- enchanted items
- other items that do not behave like normal stackable wagers

If the system rejects your item, use a different wager item.

---

## Item betting rules

When using items:

- you must be holding the item in your main hand
- you must actually own enough of that exact item
- the minimum bet is **1**
- the maximum bet is **1 stack**
- the system checks your inventory for matching items

If you try to bet more than you have, the wager will fail.

---

## Commands

Coin Flip is accessed through:

- `/cf`

The exact subcommands and usage formats depend on how the command handler is set up on the server.

If you are unsure, use the server’s help guidance for the command or ask staff.

---

## Messages and muting

Coin Flip can send you result messages when you win or lose.

You may be able to mute or unmute Coin Flip result messages. Muting only affects Coin Flip messaging and is meant for players who do not want chat feedback from the feature.

Message mute status is not guaranteed to stay saved forever, especially across restart or reload.

---

## Stats

Coin Flip tracks player stats behind the scenes.

Tracked stats may include things like:

- money wins
- money losses
- money wagered
- money won
- item wins
- item losses
- items wagered
- items won

These are internal player stats used by the feature.

---

## Rules

Use Coin Flip normally and fairly.

Do not:

- attempt to exploit bugs
- use glitched items or edge-case items to bypass item restrictions
- spam the feature in ways that disrupt the server
- intentionally abuse overflow item drops or similar behavior

If you think something paid out incorrectly, stop using the feature and contact staff.

---

## Tips

- Make sure you have enough money before using money mode.
- For item mode, hold the exact item you want to bet in your main hand.
- Use normal stackable items for item wagers.
- Leave inventory space open before item flips so winnings do not drop on the ground.
- If a wager fails, read the error message carefully. It usually tells you what is wrong.

---

## Common problems

### “Insufficient funds”
You do not have enough money for the amount you tried to bet.

### “You must hold an item in your hand”
You tried to use item mode without holding a valid item in your main hand.

### “You cannot bet non-stackable items”
The item you are trying to use is blocked by the feature.

### “You only have X of that item”
You tried to bet more of the item than you actually have.

### “Maximum bet is 1 stack”
You tried to bet more than the allowed item limit.

---

## FAQ

### Is Coin Flip player versus player?
No. Coin Flip is against the server.

### Can I use money?
Yes.

### Can I use items?
Yes, if they are valid wager items.

### Can I bet tools or armor?
No.

### Can I bet enchanted items?
No.

### What happens if my inventory is full when I win items?
Some or all winnings may be dropped at your location.

### Can I mute Coin Flip messages?
There is support for muting Coin Flip messages.

### Will my mute setting always stay saved?
Not necessarily. It may reset after restart or reload.

### Where do I start?
Use `/cf`.

---

## Limitations

- Not every item can be wagered.
- Item bets are limited to one stack.
- Full inventories can cause item winnings to drop nearby.
- Exact command syntax may vary depending on the server command setup.

---

## When to contact staff

Contact staff if:

- money or items seem to disappear incorrectly
- you believe a payout was wrong
- a wager result looks bugged
- you think you found an exploit or edge case

When reporting a problem, include:

- whether it was a money bet or item bet
- the amount wagered
- the item used, if any
- what result you expected
- what actually happened