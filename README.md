# Interactic Renewed

<!-- Badges will be added once the mod is published on Modrinth, CurseForge, and GitHub Releases -->
<!-- 
[![curseforge](https://img.shields.io/badge/-CurseForge-gray?style=for-the-badge&logo=curseforge&labelColor=orange)](https://www.curseforge.com/minecraft/mc-mods/interactic-renewed)
[![modrinth](https://img.shields.io/badge/-modrinth-gray?style=for-the-badge&labelColor=green&labelWidth=15&logo=appveyor&logoColor=white)](https://modrinth.com/mod/interactic-renewed)
[![release](https://img.shields.io/github/v/release/EmpireWarrior81/Interactic-Renewed?logo=github&style=for-the-badge)](https://github.com/EmpireWarrior81/Interactic-Renewed/releases)
-->

A maintained fork of [Interactic](https://github.com/glisco03/interactic) by glisco, updated to Minecraft 1.20.1 with several bug fixes and improvements.

## Features

All features can be individually toggled in the config.

- **Fancy Item Rendering** — Items spin while falling, at changing speed depending on how fast they fall. When on the ground they lay flat and compact stacks.
- **Enhanced Pickup and Tooltips** — Pick up items by right-clicking them, even from a few blocks away. When looking at one, its tooltip is rendered below your crosshair.
- **Item Filter** — An item that lets you control which items you want to automatically pick up. Supports white- and blacklists. Items on the list require explicit pickup via right-click or sneaking.
- **Item Throwing** — Hold the drop key to throw items. The longer you hold, the further you throw. Items with a damage modifier (swords, axes) deal damage on hit.
- **Client-Only Mode** — Automatically disables all server-side features, giving you only the enhanced rendering and tooltips.

## Changes from the original (1.20 → 1.20.1)

### Bug Fixes

- **ItemFilter slot removal ignored the amount parameter** — always deleted the entire stack regardless of how many were requested. Fixed to use `Inventories.splitStack()` so partial removals work correctly.
- **ItemFilter inventory changes were not saved** — `removeStack()` never called `markDirty()`, so changes could be lost on reload. Fixed.
- **Deprecated block outline shape API** — called `Block.getOutlineShape()` directly, which is deprecated in 1.20.1. Fixed to call `BlockState.getOutlineShape()`.
- **Mixin compatibility level was wrong** — `interactic.mixins.json` declared `JAVA_16`. Corrected to `JAVA_17`.
- **Item filter UI always registered on startup** — `HandledScreens.register` and networking ran unconditionally. Now only runs when the item filter feature is enabled in config.

### Code Improvements

- Unsafe cast in item raycasting replaced with safe `instanceof` pattern matching.
- Auto-pickup logic rewritten for clarity.

### Compatibility Updates

- Updated to Minecraft 1.20.1
- Fabric Loader >= 0.19.1
- Fabric API 0.92.7+1.20.1
- owo-lib 0.11.2+1.20
- ModMenu 7.2.2
- Java 17

## Dependencies

- [Fabric Loader](https://fabricmc.net/use/installer/) >= 0.19.1
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [owo-lib](https://modrinth.com/mod/owo-lib) >= 0.11.2

## Credits

- [glisco](https://github.com/glisco03) — original Interactic mod
- [EmpireWarrior](https://github.com/EmpireWarrior81) — 1.20.1 port and bug fixes

## License

This project follows the same license as the original Interactic mod.