# Alignments

Alignments are the Triple Alliance loyalty system. When you join the server you can pledge to one of six alignments, each tied to a campus and one of two grand alliances. Your alignment gives you a chat identity, small perks, reputation that grows over time, and a place in the season leaderboards.

You can hold exactly one alignment at a time. You can switch later, but switching may be limited by a cooldown.

## The two grand alliances

- **Concordat of the Dawn** - builders who believe the city shines brightest at first light
- **Ironclad Syndicate** - industrialists who believe the city is forged, not wished, into greatness

## The six alignments

| Alignment | Campus | Grand alliance |
|---|---|---|
| Azure Hearth | taylors | Concordat of the Dawn |
| Spirewrights | taylors | Ironclad Syndicate |
| Lagoon Covenant | sunway | Concordat of the Dawn |
| Pyramid Ascendancy | sunway | Ironclad Syndicate |
| Australis Sanctum | monash | Concordat of the Dawn |
| Zenith Collective | monash | Ironclad Syndicate |

Use `/align list` in game to see the current set - admins can add or retire alignments without a server update.

## Pledging

1. Use `/align list` to see every alignment, grouped by grand alliance.
2. Use `/align join <alignment>` to pledge, for example `/align join azure hearth`.
3. Use `/align show` to see your alignment, grand alliance, campus and join date.
4. Use `/align leave` if you ever want to be unaligned again.

Joining and leaving may be announced to the whole server, depending on server settings. Switching alignments can be rate limited by a cooldown; if it is, the game tells you how long is left.

## Reputation and ranks

Reputation measures how much you have contributed to your alignment. Admins can grant or remove it, and seasons may reset it.

Reputation unlocks ranks:

| Rank | Reputation required |
|---|---|
| Initiate | 0 |
| Associate | 25 |
| Fellow | 75 |
| Steward | 150 |
| Archon | 300 |

Higher ranks can appear in chat (for example `[Steward]` after your alignment prefix) and unlock better perks.

## Chat

Your alignment shows up in global chat automatically, for example:

```
[Concordat | Azure Hearth] [Steward] PlayerName: hello
```

- `/align chat <message>` (or `/ac <message>`) - talk privately with members of your own alignment. You must be aligned to use it.
- `/align chatspy` - admins with the chatspy permission can observe alignment chat without joining in.

## Perks

Aligned players with the right rank receive small, safe perks inside qualifying areas, refreshed automatically:

- **Swiftness** (speed) from Fellow
- **Diligence** (haste) from Steward
- **Vitality** (regeneration) from Archon
- Stewards and above also get a reduced alignment switch cooldown

Perks are removed automatically when you lose the rank, leave your alignment, or leave a qualifying area. Nothing here is dangerous or stackable to unfair levels.

## Seasons

A season is a competition period (30 days by default). When a season ends, the server snapshots every alignment's reputation, announces the top three, and starts the next season. Depending on server settings, reputation may reset to zero at season end.

Check the standings any time:

- `/align leaderboard` - top alignments by reputation
- `/align leaderboard grand` - grand alliance scores
- `/align leaderboard season` - current season leaders
- `/align leaderboard player` - where you stand inside your alignment

## Command overview

| Command | What it does |
|---|---|
| `/align help` | Show help |
| `/align list [page]` | Browse alignments |
| `/align join <alignment>` | Pledge to an alignment |
| `/align leave` | Leave your alignment |
| `/align show` | Show your alignment details |
| `/align chat <message>` | Alignment-only chat |
| `/ac <message>` | Shortcut for alignment chat |
| `/align leaderboard [view] [page]` | View leaderboards |
