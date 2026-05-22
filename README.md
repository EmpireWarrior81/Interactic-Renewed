# Interactic Renewed

<!-- Badges will be added once the mod is published on Modrinth, CurseForge, and GitHub Releases -->
<!--
[![modrinth](https://img.shields.io/badge/-modrinth-gray?style=for-the-badge&labelColor=green&logo=modrinth&logoColor=white)](https://modrinth.com/mod/interactic-renewed)
[![curseforge](https://img.shields.io/badge/-CurseForge-gray?style=for-the-badge&logo=curseforge&labelColor=orange)](https://www.curseforge.com/minecraft/mc-mods/interactic-renewed)
[![release](https://img.shields.io/github/v/release/EmpireWarrior81/Interactic-Renewed?logo=github&style=for-the-badge)](https://github.com/EmpireWarrior81/Interactic-Renewed/releases)
-->

A maintained fork of [Interactic](https://modrinth.com/mod/interactic) by glisco — bringing the beloved item interaction mod to modern Minecraft with bug fixes and continued support.

**This branch targets Minecraft 1.21.1.** — [Switch to 1.20.1](https://github.com/EmpireWarrior81/Interactic-Renewed/tree/1.20.1)

---

## What does it do?

Interactic Renewed enhances how items behave in the world, making them feel more alive and giving you better control over pickup and throwing.

### Fancy Item Rendering
Items spin as they fall, rotating faster the quicker they drop. Once they land, they lay flat on the ground instead of floating and spinning in place. Stacked items are displayed compactly on top of each other.

### Enhanced Pickup
Right-click any item on the ground to pick it up — even from a few blocks away. No more walking back and forth to grab that one item you dropped.

### Item Tooltips
When you look at an item lying on the ground, its name (and optionally full tooltip) appears just below your crosshair. You always know what you're looking at before picking it up.

### Item Filter
A craftable item that lets you control exactly which items you automatically pick up. Switch between:
- **Whitelist mode** — only auto-pick the items on the list
- **Blacklist mode** — auto-pick everything *except* items on the list

Items blocked by the filter can still be picked up manually by right-clicking or sneaking.

### Item Throwing
Hold the drop key (Q by default) to charge a throw. The longer you hold, the further the item flies. Items with attack damage modifiers — like swords and axes — deal damage on impact.

### Client-Only Mode
All server-side features disable automatically when playing on a server without the mod. You keep the fancy rendering and item tooltips everywhere.

> All features can be individually toggled in the config menu.

---

## Why Renewed?

The original [Interactic](https://modrinth.com/mod/interactic) by glisco is no longer updated. Interactic Renewed picks up where it left off:

- **Updated to modern Minecraft** — actively maintained for 1.20.1 and 1.21.1
- **ItemFilter inventory fixed** — items in the filter were sometimes lost on reload. Fixed.
- **Partial stack removal fixed** — removing items from the filter always deleted the full stack regardless of how many were requested. Fixed.
- **Deprecated API cleaned up** — several outdated API calls that could cause issues on newer Fabric versions have been replaced.
- **Mixin stability improved** — fixed wrong compatibility level declarations that could cause subtle issues.
- **Item filter UI fixed** — the filter screen and networking no longer register unconditionally on startup.

---

## Supported Versions

| Minecraft | Branch | Status |
|-----------|--------|--------|
| 1.21.1 | [1.21.1](https://github.com/EmpireWarrior81/Interactic-Renewed/tree/1.21.1) | Active |
| 1.20.1 | [master](https://github.com/EmpireWarrior81/Interactic-Renewed/tree/master) / [1.20.1](https://github.com/EmpireWarrior81/Interactic-Renewed/tree/1.20.1) | Active |

---

## Installation

**Required:**
- [Fabric Loader](https://fabricmc.net/use/installer/) >= 0.19.2
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [owo-lib](https://modrinth.com/mod/owo-lib) >= 0.12.15

---

## Credits

- [glisco](https://github.com/glisco03) — original Interactic mod
- [EmpireWarrior](https://github.com/EmpireWarrior81) — Interactic Renewed (fork, bug fixes, updates)

## License

This project follows the same license as the original Interactic mod.
