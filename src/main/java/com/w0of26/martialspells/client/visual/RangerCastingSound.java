package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Narrow client analogue of Spell Engine's repeat-until-cast-end SpellCastingSound. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class RangerCastingSound {
    private static final Map<UUID, CastLoop> ACTIVE = new HashMap<>();

    private RangerCastingSound() {}

    private enum Kind {
        RAIN, MAGIC;

        private boolean matches(Player player) {
            return this == RAIN ? MagicArrowClientVisuals.isRainOfArrowsCasting(player)
                    : MagicArrowClientVisuals.isRangerMagicArrowCasting(player);
        }

        private SoundEvent sound() {
            return this == RAIN ? MartialSoundRegistry.BOW_PULL.get()
                    : MartialSoundRegistry.GENERIC_WIND_CHARGING.get();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        Set<UUID> casting = new HashSet<>();
        if (minecraft.level != null && !minecraft.isPaused()) {
            for (Player player : minecraft.level.players()) {
                if (!player.isAlive()) continue;
                Kind kind = MagicArrowClientVisuals.isRainOfArrowsCasting(player) ? Kind.RAIN
                        : MagicArrowClientVisuals.isRangerMagicArrowCasting(player) ? Kind.MAGIC : null;
                if (kind == null) continue;
                casting.add(player.getUUID());
                CastLoop previous = ACTIVE.get(player.getUUID());
                if (previous != null && previous.kind != kind) {
                    previous.finish();
                    ACTIVE.remove(player.getUUID());
                }
                if (!ACTIVE.containsKey(player.getUUID())) {
                    CastLoop loop = new CastLoop(player, kind);
                    ACTIVE.put(player.getUUID(), loop);
                    minecraft.getSoundManager().play(loop);
                }
            }
        }
        ACTIVE.entrySet().removeIf(entry -> {
            if (casting.contains(entry.getKey())) return false;
            entry.getValue().finish();
            return true;
        });
    }

    private static final class CastLoop extends AbstractTickableSoundInstance {
        private final Player caster;
        private final Kind kind;

        private CastLoop(Player caster, Kind kind) {
            super(kind.sound(), SoundSource.PLAYERS, RandomSource.create());
            this.caster = caster;
            this.kind = kind;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.pitch = 1.0F;
            this.x = caster.getX();
            this.y = caster.getY();
            this.z = caster.getZ();
        }

        @Override public void tick() {
            if (!caster.isAlive() || caster.isRemoved() || !kind.matches(caster)) {
                finish();
                return;
            }
            x = caster.getX();
            y = caster.getY();
            z = caster.getZ();
        }

        private void finish() { stop(); }
    }
}
