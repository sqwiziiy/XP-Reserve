# Mentalitys | XP Reserve

> [Русская версия](README_RU.md)

Portable, reusable experience flasks for Fabric. XP Reserve keeps experience as exact XP points, giving survival players a practical reserve for progression, Mending, and death recovery.

## Current release

**0.1.0** — release candidate

- Minecraft **1.21.11**
- **Fabric**
- **Java 21**

## Features

- Reusable storage with exact raw-XP accounting and partial filling.
- Three progression tiers for level-30, level-50, and level-100 XP reserves.
- Mending-aware release: damaged eligible gear receives XP before the player bar.
- Clear stored-XP tooltips, five fill states, and glint only when a flask is full.
- Soul Flask death insurance that turns into a recoverable Flask III after saving XP.
- English and Russian localization, vanilla-like recipes, recipe discovery, and advancements.

## Flasks

| Item | Capacity | Level equivalent | Purpose |
|---|---:|---:|---|
| Experience Flask I | 1,395 XP | 30 | First reusable reserve |
| Experience Flask II | 5,345 XP | 50 | Upgraded reserve |
| Experience Flask III | 30,970 XP | 100 | Long-run reserve |
| Soul Flask | 30,970 XP | 100 | Death insurance; becomes Flask III after capture |

“Level equivalent” means the XP required to reach that level from zero. XP Reserve stores raw XP points, never abstract levels.

## Controls

| Control | Action |
|---|---|
| Sneak + Right Click | Store as much XP as fits |
| Right Click | Release all stored XP |

Released XP follows vanilla Mending first. Any XP not used to repair eligible Mending gear reaches the player’s XP bar.

## Soul Flask

Craft a Soul Flask from Flask III and a Totem of Undying. It can be filled and released normally, just like Flask III.

On a real death, Totem resolution happens first. If death proceeds, one fitting Soul Flask saves as much XP as it can, becomes Flask III, and drops with the player’s normal items. Recover it from the drops: lava, the void, or item despawn can still destroy it. `keepInventory` does not consume the insurance.

## Recipes

### Experience Flask I

```text
A L A
L B L
A L A

A = Amethyst Shard
L = Lapis Lazuli
B = Glass Bottle
```

### Upgrades

```text
Flask I   + Diamond Block    -> Flask II
Flask II  + Recovery Compass -> Flask III
Flask III + Totem of Undying -> Soul Flask
```

Stored XP is preserved when upgrading a valid flask.

## Requirements

- Minecraft **1.21.11**
- Fabric Loader **0.19.3** or a compatible loader version
- Fabric API **0.141.6+1.21.11**
- Java **21**

## Installation

1. Install Fabric Loader for Minecraft 1.21.11.
2. Install the matching Fabric API.
3. Place the XP Reserve JAR in the Minecraft `mods` folder.
4. Launch Minecraft with Java 21.

## Links

- [GitHub repository](https://github.com/sqwiziiy/XP-Reserve)
- [Issues](https://github.com/sqwiziiy/XP-Reserve/issues)
- [Documentation](https://sqwiziiy.github.io/XP-Reserve/) — expected after GitHub Pages is enabled

## Supported versions

| Minecraft | Loader | Java | Status |
|---|---|---:|---|
| 1.21.11 | Fabric | 21 | Current release candidate |

Minecraft 1.21.1 and 1.20.1 are not supported yet.

## License

License: [CC0 1.0 Universal](LICENSE).
