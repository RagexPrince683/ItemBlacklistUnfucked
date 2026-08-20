# Changelog

## PR: Block extraction of banned crafting results

- Banned crafting results can no longer be extracted while a crafting GUI
  remains open.
- Blocked results remain visible using the existing banned-item representation.
- Crafting-result click prevention is server-authoritative, including direct
  window-click packets and every vanilla container click mode.

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
