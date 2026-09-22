# Warrior W8 — Final fidelity / integration audit

Status: **PASS / FROZEN**

Primary Rogues source freeze: `89ba33ad29adc42d7306660f5b74f28bd17b8ffa`.

Exact Spell Engine dependency freeze used for inherited behavior archaeology:
`1.10.5.034+1.20.1`, commit
`270a6d61b00f241c1c8adb87573ad6de2d547f66`.

Accepted Rogue-complete MartialSpells base:
`3c264221bf68e5a1b369f29639dc3d78651cc5c8`.

Frozen W7 tip:
`e3017fd7a417160585e91f4580430ca03eb2350f`.

W8 is a validation-only checkpoint. No W0-W7 gameplay implementation is intentionally changed.

## Frozen source manifest

The six Warrior source spell JSON blobs at the frozen Rogues commit are:

- `charge.json` — `8a617545ec40b5ef824011104a041983906090d5`
- `shout.json` — `2eee2784d2552489b4c6d059b36b450851187260`
- `throw_net.json` — `36bf5e443ee2e780470693122626c593c4ad5528`
- `throw.json` — `fbfe61ffcaa5add34caf0ce16ef6122e5f3d9445`
- `mortal_strike.json` — `063bd0fc9dfbc2bfc727e30477e0cd4816a7ee6c`
- `last_stand.json` — `8c266eeb644b908590b2a72bf9b98cb9cd89e31e`

The final Martial IDs are:

```text
martial_spells:charge
martial_spells:demoralizing_shout
martial_spells:throw_net
martial_spells:shattering_throw
martial_spells:mortal_strike
martial_spells:last_stand
```

There are no additional Warrior techniques in this port phase.

## Final fidelity result before runtime gate

Repository/source review found no unresolved mechanical drift after W7.

The final contract remains:

- **Charge** — source mechanics preserved except the explicit user-approved duration override from 2 sec to 10 sec.
- **Demoralizing Shout** — 12-block / 0.5 vertical area, ADD-stacking Demoralized, source health gate, separate 5% physical-melee damage, zero knockback, 12 sec cooldown.
- **Throw Net** — 0.45 sec charge, 20% minimum release, 0.5 output scaling, sticky target, 22-block full range, source projectile/net/root behavior, 12 sec cooldown.
- **Shattering Throw** — 0.5 sec charge, 20% minimum release, full proportional output, 24-block full range, held-item projectile, one bounce, source Shatter behavior, 8 sec cooldown.
- **Mortal Strike** — exact 0.5 sec vertical windup/release pair, source-shaped delayed 3-block melee, +50% vanilla weapon strike, exact local Spell Engine .034 Bleed translation, 15 sec cooldown.
- **Last Stand** — exact five source channel pulses at ticks 5/15/25/35/45, release at 50, five-stack max-health/knockback-resistance effect, post-stack heal, proportional early-release cooldown, 60 sec full cooldown.

## Presentation fidelity boundary

The port intentionally does **not** claim every Spell Engine presentation primitive is byte-for-byte identical.

Accepted dependency-free translations:

- Charge uses Iron's instant-cast pose/local VFX instead of importing `spell_engine:one_handed_area_release`; the source speed-sign popup was explicitly omitted by user preference.
- Demoralizing Shout uses Iron's instant-cast pose and local Rage-smoke area presentation instead of Spell Engine's exact shout/area-effect renderer.
- Throw Net and Shattering Throw use Iron's LONG/throw animation transport rather than importing Spell Engine's exact charge/release animation runtime.
- Their gameplay timing, targeting, projectile behavior, sounds, models/textures where retained, and effect mechanics remain the accepted source-fidelity contract.
- Mortal Strike and Last Stand use the exact relevant Spell Engine 1.10.5.034 PlayerAnimator assets imported locally.

This distinction is intentional and is not a W8 blocker unless runtime regression is found.

## Repository integration review

The Warrior branch was compared against accepted Rogue-complete base
`3c264221bf68e5a1b369f29639dc3d78651cc5c8`.

At the W7 frozen tip it is:

```text
ahead:  26 commits
behind: 0 commits
```

The diff is confined to Warrior classes/support, shared registries/networking/damage resources, Warrior resources, mixins required by charged/root behavior, and checkpoint/audit documentation. No accepted Rogue spell implementation was rewritten.

Final network protocol is `14`, containing both the W4 Netted visual packet and W7 early-release Last Stand animation packet.

No Spell Engine or Spell Power runtime dependency is introduced.

## W8 validation gate

From a clean local checkout:

```powershell
git switch feature/warrior-spells-port
git pull --ff-only origin feature/warrior-spells-port

python .\tools\audit-warrior-w8.py

.\gradlew clean build
.\gradlew runClient
```

The static audit must end with:

```text
Warrior W8 STATIC FINAL AUDIT PASS
Source inventory: exactly 6 Warrior techniques.
Mechanics: W2-W7 frozen contracts preserved; Charge retains approved 10-second override.
Assets: exact imported icons/sounds and W6/W7 Spell Engine .034 animations verified.
Dependencies: no Spell Engine / Spell Power runtime dependency introduced.
Integration: accepted Rogue baseline still present; final Warrior network protocol = 14.
Runtime/build validation is still required before W8 can be frozen PASS.
```

## Runtime regression matrix

Validate the six Warrior techniques once more as a completed set:

1. **Charge** — 10 sec duration, +50% base Movement Speed, +50% base Knockback Resistance, refresh/no stack, 12 sec cooldown.
2. **Demoralizing Shout** — valid hostile area targeting, separate damage vs debuff health gate, ADD stacks through Demoralized VI, no induced knockback.
3. **Throw Net** — sub-20% fizzle; valid partial/full releases; sticky homing; physical net projectile/VFX; 3 sec root that still permits attacking/item use/casting; knockback immunity while Netted.
4. **Shattering Throw** — charge scaling, held-item visual without consuming the item, one block bounce, direct hit + Shattered Armor gate, persistent blood presentation.
5. **Mortal Strike** — overhead windup -> vertical slash, source hitbox/timing, normal weapon hooks with +50% Attack Damage, Bleed application and movement-scaled lethal ticks.
6. **Last Stand** — rooted 2.5 sec channel, five pulses, Last Stand V at full channel, 20/20 baseline reaches the already-validated source-faithful 36/40 HP result, early release preserves earned pulses and produces proportional cooldown.

Then perform a brief cross-regression smoke:

- one accepted Rogue technique still casts normally;
- one pre-existing Monk technique still casts normally;
- Warrior spell wheel/tag visibility contains all six techniques;
- no duplicate Warrior technique appears;
- reconnect/reload does not leave Netted or Last Stand client presentation stuck.

If this build is immediately going onto the private dedicated server, also perform one server startup + client-join smoke before calling the combined work release-ready.

W8 is **PASS / FROZEN**. The user explicitly confirmed the final audit/build/runtime gate passed. The Warrior technique port into MartialSpells is complete.
