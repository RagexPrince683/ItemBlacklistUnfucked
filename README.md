# ItemBlacklist

Mod does not require D3Core.

Banned recipe outputs remain visible in player and crafting-table result slots
as banned items, but cannot be extracted. The server enforces this restriction
before vanilla handles container clicks, so it does not depend on the client or
on NEI. Clicking a banned crafting result displays the configured `message`
from `config/ItemBlacklist.cfg` in chat.

![Now is not the time to use that.](meme.jpg)

## Development mods

Place ordinary production Minecraft 1.7.10 mod JARs in `devmods/*.jar`. Gradle
automatically remaps these JARs from production names into the MCP development
environment through RetroFuturaGradle.

Only place JARs that are already deobfuscated for development in
`devmods/deobf/*.jar`; Gradle loads these directly without remapping them. Both
locations are available to development compilation, `runClient`, and `runServer`.
The local `devmods/` directory is ignored by Git, and none of its contents are
included in release JARs or published dependencies.

## Commands
### `/blockitem` (also `/itemblacklist` or `/blacklist`)
The main command, it has an ingame help. (`/blockitem help`)

When no item is named, `/itemblacklist ban` bans the held registry item across
all metadata variants. `/itemblacklist meta` instead bans only the exact
metadata value of the held stack, and `/itemblacklist unmeta` removes that exact
held-item ban. All three commands default to the player's current dimension and
accept the existing dimension list/range syntax or `__GLOBAL__` (for example,
`/itemblacklist meta __GLOBAL__`). Explicit bans such as
`/itemblacklist ban minecraft:dye:15` remain supported.

`ban` and `meta` accept an optional schedule suffix. Without one, the ban is
permanent (and applying the permanent ban again clears an old expiration):

```text
/itemblacklist ban timer 30m
/itemblacklist meta timer 2h
/itemblacklist ban minecraft:tnt timer 7d
/itemblacklist ban __GLOBAL__ minecraft:tnt date 12-31-2026 11:59pm
```

Relative schedules use `timer <duration>`. Durations may combine `y` (years),
`d` (days), `h` (hours), `m` (minutes), and `s` (seconds), such as `1d12h` or
`2h30m`. Years and days are calendar operations in the server system time zone.
Absolute schedules use `date <MM-dd-yyyy> <time>` with a 12-hour time such as
`7pm` or `7:30pm`; they are interpreted in the server system time zone. The
schedule must be the final command suffix. Repeating a scheduled ban updates
its expiration rather than creating a duplicate, while `unban` and `unmeta`
remove it immediately.

### `/unpack`
Lets anyone unpack there own inventory. Useful for items required in crafting. **Can be disabled in the config**

## Pack vs World bans

Since v1.2 there is 2 possible blacklists:

**They both apply at the same time!**

- World:
    Changeable trough the command `/blockitem`. Also a file: `<world data>/ItemBlacklist.json`.
- Pack:
    Only via the file: `config/ItemBlacklist.json`.

You cannot change the pack config in the game, its meant for pack makers who want to ban items from use for all servers.
If you as a server owner then want to ban more items, just use the command.

This also makes it easy to maintain the 2 lists. The pack maker can update the Pack list, and not override bans made by server owners.
And server owners can further restrict items without much effort. The only hard thing comes when a server owner wants to overrule a pack maker's ban.
You will have to manually edit the Pack JSON file (`config/ItemBlacklist.json`) to remove the entry, and do that every pack update.

For this reason I recommend pack makers to provide an extra file, if they want to have optional/recommended extra bans, 
and put that file somewhere safe (so it won't override `world/ItemBlacklist.json`!), and attach a note to your server download/info page.

## JSON format

Example format: 
```javascript
{
  "__GLOBAL__": [
    {
      "item": "minecraft:dye",
      "meta": 15
    }
  ],
  "0": [
    {
      "item": "minecraft:dye",
      "meta": "*"
    }
  ]
}
```

This file means that:
Bonemeal is `minecraft:dye` metadata `15` in Minecraft 1.7.10, so this bans
bonemeal everywhere without banning other dye variants. All dye metadata
variants are banned in the overworld (dimension 0).

`meta` is the canonical metadata property and may be either an integer or `"*"`
for all metadata variants. Legacy JSON entries using `damage` remain readable;
entries with neither property default to `"*"`. If a file is rewritten, entries
are saved with `meta`. Metadata is not NBT, and item names remain Forge registry
IDs rather than numeric IDs.

A scheduled world entry additionally has an optional `expiresAt` property,
stored as an ISO-8601 UTC instant (for example,
`"expiresAt": "2026-08-25T23:30:00Z"`). Entries without it remain permanent,
so existing files need no migration. Absolute instants ensure server downtime
counts toward expiration and schedules survive restarts. Expired entries never
match a ban and are removed from the world JSON by periodic server cleanup.

### Ranges / Multiple dimensions

If you want to specify multiple dimensions (for example, "0, -1 and 1", you can do so by using a comma `,` to separate the numbers: `0,-1,1`.
You can also specify a range of dimensions with the pound/hash `#` symbol: `10#100`.

You can create multiple overlapping ranges, they should merge. Just don't make 2 identical ranges.
