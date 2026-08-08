# XP Reserve agent guide

## Product principles

XP Reserve is not intended to be a generic infinite XP tank. Its planned identity is:

- survival-oriented experience insurance;
- reusable portable flasks;
- fixed progression tiers;
- correct storage in **raw XP points**, never naive level subtraction/addition;
- risk/reward rather than free permanent XP protection;
- vanilla-like interactions and visuals;
- correct Mending interoperability;
- no XP duplication under any circumstances.

## Current version policy

- Primary/current target: Minecraft 1.21.11 Fabric.
- Java 21.
- Do not add 1.21.1 or 1.20.1 compatibility in this source branch/project during current development.
- Ports happen only after the 1.21.11 implementation stabilizes.
- Do not introduce multi-loader abstractions.

## Engineering rules

- Server-authoritative gameplay state.
- Never trust client-side XP values for mutations.
- Dedicated-server compatibility is mandatory.
- Keep client imports out of common code.
- Prefer Minecraft/Fabric APIs over Mixins when a proper API exists.
- Add Mixins only when justified.
- Avoid unrelated refactors.
- No hidden XP loss or gain.
- XP transfer math must be centralized and testable.
- Persistent per-item state must use the modern 1.21.11 item/data-component approach rather than old NBT patterns where the current API provides the proper mechanism.
- Do not store "levels" as the canonical currency.
- Public behavior must be documented when implemented.
- Build must be clean before declaring a phase complete.

## Validation rules

At minimum before completing any later implementation phase:

```bash
./gradlew build
```

For gameplay changes, add focused automated tests where practical and document any manual in-game acceptance steps.
