# Changelog

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
