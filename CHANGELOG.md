# Changelog

## 0.1.0 — First release

### Added

- Experience Flask I, II, and III with capacities of 1,395, 5,345, and 30,970 raw XP.
- Partial filling, exact stored-XP tooltips, five fill visuals, and full-flask glint.
- Mending-aware release that repairs eligible gear before returning remaining XP to the player.
- Soul Flask death insurance, crafted from Flask III and a Totem of Undying.
- Vanilla-like recipes, recipe-book discovery, XP Reserve advancements, English/Russian localization, and restrained interaction sounds.
- Optional JEI and REI crafting-recipe display compatibility.

### Correctness

- Player XP is handled exactly from the visible level/progress state, including command-driven states where the vanilla total field is stale.
- Flask transfers, upgrades, Mending release, and Soul capture preserve XP according to their documented contracts.
