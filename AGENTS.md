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

## Mandatory phase reports

All future coding agents must create or update a private phase report after every implementation phase or meaningful sub-phase, **before declaring the task complete**. Store reports under:

```text
docs/reports/PHASE_<NN>_<SHORT_NAME>.md
```

Examples:

```text
docs/reports/PHASE_00_FOUNDATION.md
docs/reports/PHASE_01_XP_CORE.md
docs/reports/PHASE_02_FLASK_I.md
docs/reports/PHASE_03_TIER_SYSTEM.md
docs/reports/PHASE_02_1_FIX_WITHDRAWAL.md
```

Reports are local/private development artifacts and remain ignored by Git through `/docs/`. They must document work actually completed, never planned work presented as complete. Failed or partial work must be reported honestly, with commands and validation results recorded; do not claim `PASS` without evidence.

Every report must use this minimum structure:

```markdown
# Phase XX — <Name>

## Status
PASS / PARTIAL / BLOCKED

## Scope
## Implemented
## Files changed
## Technical decisions
## Validation
## Test results
## Artifacts
## Known issues
## Deviations
## Git
## Next phase
```

State `NOT RUN` for every test that was not run. Record relevant failures even if they were later fixed, without dumping huge logs. Document toolchain changes, gameplay behavior changes, compatibility or save-format changes, dependencies, and any XP duplication/data-loss risk. Phases involving XP math or item persistence must explicitly record the invariants checked.
