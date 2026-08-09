# Mentalitys | XP Reserve

XP Reserve is a Fabric mod for Minecraft 1.21.11 (Java 21) that adds reusable, survival-focused experience flasks. XP is stored as exact raw points, not as levels, so deposits, withdrawals, and upgrades do not lose or duplicate experience.

## Flasks and controls

| Item | Capacity | Use |
|---|---:|---|
| Experience Flask I | 1,395 XP (level 30 from zero) | Store and release XP |
| Experience Flask II | 5,345 XP (level 50 from zero) | Store and release XP |
| Experience Flask III | 30,970 XP (level 100 from zero) | Store and release XP |
| Soul Flask | 30,970 XP | Death insurance; becomes Flask III after capture |

- Sneak + Right Click stores as much XP as fits.
- Right Click releases stored XP through vanilla Mending first, then returns any remainder to the player.
- A Soul Flask captures player XP after a real death has been entered, before vanilla death XP is calculated. It captures at most one flask per death and becomes Flask III on success.

## Progression

- Craft Flask I with amethyst shards, lapis lazuli, and a glass bottle.
- Upgrade Flask I with a diamond block, Flask II with a recovery compass, and Flask III with a Totem of Undying.
- Recipes unlock progressively in the recipe book; the compact XP Reserve advancement chain tracks the four flask milestones.

The mod is designed to preserve XP exactly across its own storage operations and to cooperate with vanilla Mending. Future ports are considered only after this Fabric 1.21.11 target is stable.
