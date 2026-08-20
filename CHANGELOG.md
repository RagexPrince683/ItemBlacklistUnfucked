# Changelog

## PR: Add timed and scheduled item unbans

- Added optional `timer <duration>` and `date <MM-dd-yyyy> <time>` suffixes to
  world `ban` and `meta`, including rescheduling and conversion to permanent.
- Persisted UTC expirations as optional `expiresAt` entry properties while
  retaining permanent-entry and legacy `damage` JSON compatibility.
- Added server wall-clock matching, one-second server-thread cleanup,
  restart/reload cleanup, and immediate online inventory/container refreshes.
- Documented scheduling units, syntax, time-zone behavior, and persistence.

## PR: Add held-item metadata ban commands and canonical JSON metadata

- Added `/itemblacklist meta` and `/itemblacklist unmeta` to create and remove
  exact held-item metadata bans across the existing dimension selectors.
- Kept held-item `/itemblacklist ban` wildcard behavior and explicit registry
  item metadata syntax.
- Made `meta` the canonical JSON property while retaining validated support for
  legacy `damage` entries and wildcard defaults.
- Documented metadata-specific dye and bonemeal bans.

## PR: Show the banned-item message for crafting clicks

- Display the configured banned-item message when a player attempts to take a
  banned crafting result.
- Document the crafting-click feedback and its configuration source.

## PR: Fix GTNH coremod class resolution

- Made the configured coremod class relative to `modGroup`, allowing the GTNH
  convention plugin to resolve the existing loading plugin correctly.

## PR: Enforce banned crafting results with a Forge coremod

- Banned crafting results can no longer be extracted while a crafting GUI
  remains open.
- Blocked results remain visible using the existing banned-item representation.
- Crafting-result click prevention is server-authoritative, including direct
  window-click packets and every vanilla container click mode.
- Banned crafting-result extraction is enforced through a small Forge 1.7.10
  coremod hook.

## PR: Fix GTNH build configuration

- Corrected the GTNH `modGroup` to match the existing
  `net.doubledoordev.itemblacklist` source package.
- Completed the GTNH convention properties, including the stable MCP mapping
  channel and disabled unused publishing, shadowing, and mixin systems.
- Replaced the legacy `mcmod.info` version tokens with GTNH project property
  tokens.
- Preserved the existing local `devmods` remapping and deobfuscated JAR support.

## PR: Add devmods support

- Migrated the development build from legacy ForgeGradle 1.2 to the GTNH
  convention and RetroFuturaGradle stack.
- Added automatic MCP remapping for production JARs in `devmods/`.
- Added direct development loading for deobfuscated JARs in `devmods/deobf/`.
- Kept all development mod JARs local and out of release artifacts.
