# Martial Spells

Martial Spells is a Forge 1.20.1 addon for
[Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks).

It adds martial-focused spells designed for tanks, bruisers, duelists, and
assassins. These spells support melee combat rather than replacing it with
conventional ranged magic.

## Current Content

### Guardian's Cry

Guardian's Cry is an area taunt spell that:

- Affects hostile mobs within a fixed radius.
- Forces affected mobs to target the caster for a limited duration.
- Displays a duration indicator on the caster.
- Restores each mob's previous valid target when the taunt expires.
- Supports target restoration between players and ordinary mobs.

## Development Status

Martial Spells is currently in early development and is being created for a
custom Minecraft modpack.

Current development target:

- Minecraft 1.20.1
- Forge 47.4.10
- Java 17
- Iron's Spells 'n Spellbooks 1.20.1-3.16.2

## Planned Direction

Future spells will focus on martial archetypes such as:

- Tanks
- Bruisers
- Duelists
- Assassins

The intended design is for spells to enhance positioning, threat management,
defensive timing, melee pressure, and battlefield control.

## Building from Source

1. Clone the repository.
2. Install JDK 17.
3. Open the project as a Gradle project in IntelliJ IDEA.
4. Generate the development runs:

```bash
gradlew genIntellijRuns