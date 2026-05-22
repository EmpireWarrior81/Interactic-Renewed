# Interactic Renewed

<!-- Badges will be added once the mod is published on Modrinth, CurseForge, and GitHub Releases -->
<!-- 
[![curseforge](https://img.shields.io/badge/-CurseForge-gray?style=for-the-badge&logo=curseforge&labelColor=orange)](https://www.curseforge.com/minecraft/mc-mods/interactic-renewed)
[![modrinth](https://img.shields.io/badge/-modrinth-gray?style=for-the-badge&labelColor=green&labelWidth=15&logo=appveyor&logoColor=white)](https://modrinth.com/mod/interactic-renewed)
[![release](https://img.shields.io/github/v/release/EmpireWarrior81/Interactic-Renewed?logo=github&style=for-the-badge)](https://github.com/EmpireWarrior81/Interactic-Renewed/releases)
-->

A maintained fork of [Interactic](https://github.com/glisco03/interactic) by glisco, updated to Minecraft 1.21.1 with bug fixes and improvements.

> **Other versions:** [1.20.1](https://github.com/EmpireWarrior81/Interactic-Renewed/tree/master)

## Features

All features can be individually toggled in the config.

- **Fancy Item Rendering** — Items spin while falling, at changing speed depending on how fast they fall. When on the ground they lay flat and compact stacks.
- **Enhanced Pickup and Tooltips** — Pick up items by right-clicking them, even from a few blocks away. When looking at one, its tooltip is rendered below your crosshair.
- **Item Filter** — An item that lets you control which items you want to automatically pick up. Supports white- and blacklists. Items on the list require explicit pickup via right-click or sneaking.
- **Item Throwing** — Hold the drop key to throw items. The longer you hold, the further you throw. Items with a damage modifier (swords, axes) deal damage on hit.
- **Client-Only Mode** — Automatically disables all server-side features, giving you only the enhanced rendering and tooltips.

## Changes from 1.20.1 → 1.21.1

### API Migrations

- **Networking rewritten** — `PacketByteBufs` removed. All packets migrated to `CustomPayload` records with `PacketCodec` (4 new payload classes).
- **Item data migrated to DataComponents** — `ItemStack.getOrCreateNbt()` replaced throughout with `DataComponentTypes.CUSTOM_DATA` + `NbtComponent`.
- **ItemFilter inventory serialization rewritten** — `Inventories.readNbt/writeNbt` replaced with custom read/write using item registry IDs, compatible with 1.21.1.
- **Attribute API updated** — `ItemStack.getAttributeModifiers()` replaced with `stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS)`. `Operation.ADDITION` renamed to `Operation.ADD_VALUE`.
- **Player reach distance updated** — `ClientPlayerInteractionManager.getReachDistance()` replaced with `EntityAttributes.PLAYER_ENTITY_INTERACTION_RANGE`.
- **Identifier API updated** — `new Identifier(namespace, path)` replaced with `Identifier.of(namespace, path)` everywhere.

### Rendering Changes

- **InGameHud mixin updated** — `renderCrosshair` signature now includes `RenderTickCounter`. Tooltip rendering adjusted for 1.21.1 `TooltipContext` and `TooltipType` API.
- **Item renderer shadow removed** — `getRenderedAmount(ItemStack)` was renamed in 1.21.1. Logic inlined directly.
- **Frame duration tracking rewritten** — `MinecraftClient.getLastFrameDuration()` removed. Replaced with a `WorldRenderEvents.START` hook that tracks frame time via `Util.getMeasuringTimeMs()`.

### UI Changes

- **ItemFilter screen buttons updated** — `TexturedButtonWidget` constructor changed. Replaced with `ButtonWidget.builder()`.
- **Screen background rendering updated** — `renderBackground(context)` now requires mouse coordinates.

### Compatibility Updates

- Updated to Minecraft 1.21.1
- Fabric Loader >= 0.19.2
- Fabric API 0.116.12+1.21.1
- owo-lib 0.12.15.4+1.21
- ModMenu 11.0.4
- Java 21

## Dependencies

- [Fabric Loader](https://fabricmc.net/use/installer/) >= 0.19.2
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [owo-lib](https://modrinth.com/mod/owo-lib) >= 0.12.15

## Credits

- [glisco](https://github.com/glisco03) — original Interactic mod
- [EmpireWarrior](https://github.com/EmpireWarrior81) — 1.20.1 port, bug fixes, and 1.21.1 update

## License

This project follows the same license as the original Interactic mod.
