# MartialSpells Ranger API — Armory CP5 handoff

Status: final AR6 handoff; **AR6 RUNTIME PASS**, user-confirmed.
No equipment integration is implemented in MartialSpells. The dependency
direction is consumer → MartialSpells, with no reverse dependency.

## Public boundary

Import only `com.w0of26.martialspells.api.RangerCompatibility`.
Both callbacks use Java `ToIntFunction<net.minecraft.world.entity.player.Player>`.
Register during Forge common setup via `event.enqueueWork`. Query execution is
on the logical server thread; callbacks must not access client classes, mutate
player/inventory state, or schedule gameplay. Use server-authoritative equipment
state. Each setter replaces the previous callback; registrations do not stack.
An adapter combining several bonuses must aggregate them in its own callback.
Reset to neutral by registering `player -> 0`; null providers are rejected.

## A. Barrage extra penetrable targets

```java
RangerCompatibility.setBarrageExtraPenetrableTargetsProvider(
    player -> hasConsumerBarrageBonus(player) ? 1 : 0);
```

`hasConsumerBarrageBonus` is consumer-owned logic, not a MartialSpells method.

- Setter: `public static void setBarrageExtraPenetrableTargetsProvider(ToIntFunction<Player>)`.
- Query: `public static int extraBarragePenetrableTargets(Player)`; the spell
  invokes this internally, so consumers need only register the provider.
- Default output: **0**; valid integer outputs **0..127**. Invalid values throw
  `IllegalArgumentException`; client-side queries throw `IllegalStateException`.
- Invocation: once per Barrage launch that passes weapon/ammunition validation,
  before ammo-created arrow preparation, at release / +2 / +4 ticks. A failed
  ammunition lookup does not query the provider. No cast-time bonus snapshot.
- Output is additive to baseline crossbow Piercing, then saturates safely:
  `effectivePierceLevel = min(127, baselinePierceLevel + extraTargets)`.
  Addition uses a widened integer before conversion to vanilla's signed byte.
- Vanilla pierce level N permits **N+1 distinct entity hits**. A Strider output
  **+1** therefore permits exactly one additional target unless the byte ceiling
  is already saturated: baseline 0 → level 1 → two total distinct targets;
  baseline 3 → level 4 → five total. Default 0 adds no source bonus.
- Only arrows constructed for Barrage receive this query. Ordinary bow/crossbow
  arrows and independent Entangling Arrow are unaffected. Actual ammunition
  type, baseline enchantments, existing hit bookkeeping and saved metadata stay
  on the projectile. Consumers never manipulate arrow NBT or mixin internals.

## B. Spirit Wolf fixed cooldown adjustment

```java
RangerCompatibility.setSpiritWolfCooldownAdjustmentProvider(
    player -> hasConsumerWolfBonus(player) ? -100 : 0);
```

- Setter: `public static void setSpiritWolfCooldownAdjustmentProvider(ToIntFunction<Player>)`.
- Default output **0**; only valid outputs **0** and **-100** ticks. Other
  callback results throw `IllegalArgumentException` at cooldown evaluation.
- Invocation: Spirit Wolf's `SpellCooldownAddedEvent.Pre` listener, at LOWEST
  priority, replaces Iron's already-reduced effective cooldown on the server.
- Final ticks: `max(1, 900 + adjustment)`; the allowed values yield exactly
  **900** or **800**. No ordinary Cooldown Reduction attribute is consulted.
- Future Riftstalker output **-100** is therefore exactly 45 → 40 seconds.
- No arbitrary cooldown setter, spell/entity class access, or source-mana
  dependency is required. Other mods that cancel the event or overwrite it at
  the same/later ordering remain a Forge compatibility boundary.

## Maven Local consumption

Coordinates from `gradle.properties` and publication `mavenJava`:

**`com.w0of26:martial_spells:1.20.1-0.1.0`**

Artifacts: main reobfuscated Forge 1.20.1 jar with resources/API; `:dev` mapped
jar with the same resources/API; `:sources` main Java sources. The existing
version is unchanged. Refresh the local publication after source changes.

ForgeGradle consumer:

```groovy
repositories { mavenLocal() }
dependencies {
    implementation fg.deobf('com.w0of26:martial_spells:1.20.1-0.1.0')
}
```

The published POM intentionally does not export this project's development
fixtures or runtime-only testing mods. The consumer supplies Forge and its
normal dependency setup; a running integration also requires MartialSpells'
existing required mods from `META-INF/mods.toml`. Do not copy a jar manually.
The mapped `:dev` classifier is suitable for a Java compile fixture using an
already-mapped Minecraft/Forge classpath; do not reobfuscate it again.

Publish with inspected task `publishMavenJavaPublicationToMavenLocal`. On this
machine the explicit local repository is `C:\Users\Ian\.m2\repository`, passed
as `-Dmaven.repo.local=C:\Users\Ian\.m2\repository` to publication and probe.
Consumers using that override must resolve the same repository. No remote
publication is part of AR6.

## Validation boundary

`tools/probe-archers-ranger-api.py` generates an ignored fixture under `build/`,
resolves the mapped classifier from Maven Local and compiles a consumer that
imports only this public API and Java types. No MartialSpells source output is
on that consumer classpath. It checks production/source/dev artifact contents
as well. It is a compile probe, not equipment gameplay or a Minecraft launch.
Equipment-side +1 and -100 runtime validation belongs to Armory CP5.

AR6 validation: local publication PASS; main/dev/sources API/resource checks
PASS; isolated external Java compile PASS. Final Ranger runtime regression is
user-confirmed PASS, including neutral Barrage piercing and fixed Wolf cooldown.
The public API is accepted for CP5 consumption. No Armory source or gameplay was
changed for the probe, and CP5 has not begun.
