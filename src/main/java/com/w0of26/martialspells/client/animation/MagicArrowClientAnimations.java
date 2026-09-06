package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class MagicArrowClientAnimations {
    private static final ResourceLocation CROSSBOW_HOLD_RIGHT =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "magic_arrow_crossbow_hold_right"
            );
    private static final ResourceLocation CROSSBOW_HOLD_LEFT =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "magic_arrow_crossbow_hold_left"
            );

    private static final Map<UUID, HumanoidArm> PLAYING =
            new HashMap<>();

    private MagicArrowClientAnimations() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            PLAYING.clear();
            return;
        }

        for (Player rawPlayer : minecraft.level.players()) {
            if (rawPlayer instanceof AbstractClientPlayer player) {
                update(player);
            }
        }
    }

    private static void update(AbstractClientPlayer player) {
        if (!MagicArrowClientVisuals.isMagicArrowCasting(player)) {
            stop(player);
            return;
        }

        HumanoidArm crossbowArm =
                MagicArrowClientVisuals.getCrossbowArm(player);

        if (crossbowArm == null) {
            /*
             * Bow casting needs no override here. Iron's own
             * charge_arrow animation remains visible underneath this
             * layer and supplies the normal bow-drawing body pose.
             */
            stop(player);
            return;
        }

        play(player, crossbowArm);
    }

    @SuppressWarnings("unchecked")
    private static void play(
            AbstractClientPlayer player,
            HumanoidArm crossbowArm
    ) {
        UUID id = player.getUUID();
        HumanoidArm current = PLAYING.get(id);

        if (current == crossbowArm) {
            return;
        }

        ModifierLayer<IAnimation> layer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess
                        .getPlayerAssociatedData(player)
                        .get(MagicArrowRangedAnimationLayer.LAYER_ID);

        if (layer == null) {
            return;
        }

        ResourceLocation animationId =
                crossbowArm == HumanoidArm.RIGHT
                        ? CROSSBOW_HOLD_RIGHT
                        : CROSSBOW_HOLD_LEFT;

        var animation =
                PlayerAnimationRegistry.getAnimation(animationId);

        if (animation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Magic Arrow crossbow animation {}",
                    animationId
            );
            return;
        }

        layer.setAnimation(new KeyframeAnimationPlayer(animation));
        PLAYING.put(id, crossbowArm);
    }

    @SuppressWarnings("unchecked")
    private static void stop(AbstractClientPlayer player) {
        UUID id = player.getUUID();

        if (PLAYING.remove(id) == null) {
            return;
        }

        ModifierLayer<IAnimation> layer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess
                        .getPlayerAssociatedData(player)
                        .get(MagicArrowRangedAnimationLayer.LAYER_ID);

        if (layer != null) {
            layer.setAnimation(null);
        }
    }
}
