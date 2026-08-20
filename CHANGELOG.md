# Changelog

## PR: Add devmods support

- Migrated the development build from legacy ForgeGradle 1.2 to the GTNH
  convention and RetroFuturaGradle stack.
- Added automatic MCP remapping for production JARs in `devmods/`.
- Added direct development loading for deobfuscated JARs in `devmods/deobf/`.
- Kept all development mod JARs local and out of release artifacts.
