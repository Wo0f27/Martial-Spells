package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.MartialSpells;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side Ender charge particles for Magic Arrow. The real Magic
 * Arrow projectile remains owned and spawned by Iron's Spells only when
 * casting completes.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class MagicArrowChannelParticles {
    private MagicArrowChannelParticles() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }

        RandomSource random = minecraft.level.random;

        for (Player player : minecraft.level.players()) {
            if (!MagicArrowClientVisuals.isMagicArrowCasting(player)) {
                continue;
            }

            float progress = MagicArrowClientVisuals.getCastProgress(player);
            int count = 1 + MthHelper.floorToInt(progress * 2.0F);

            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 0.45D + random.nextDouble() * 0.55D;
                double x = player.getX() + Math.cos(angle) * radius;
                double y = player.getY()
                        + 0.15D
                        + random.nextDouble() * Math.max(1.0D, player.getBbHeight());
                double z = player.getZ() + Math.sin(angle) * radius;

                double vx = (player.getX() - x) * 0.025D;
                double vy = 0.01D + random.nextDouble() * 0.025D;
                double vz = (player.getZ() - z) * 0.025D;

                minecraft.level.addParticle(
                        ParticleHelper.UNSTABLE_ENDER,
                        x,
                        y,
                        z,
                        vx,
                        vy,
                        vz
                );
            }
        }
    }

    private static final class MthHelper {
        private MthHelper() {
        }

        private static int floorToInt(float value) {
            return (int) Math.floor(value);
        }
    }
}
