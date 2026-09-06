package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.client.visual.MagicArrowClientVisuals;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
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

/**
 * Replaces Iron's default Magic Arrow casting pose with the dedicated
 * ranged-weapon poses used by Martial Spells.
 *
 * Bow casts use the Bow Pulling Animation keyframes supplied by MaBroxx.
 * Crossbow casts keep the already-tested loaded-crossbow hold.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class MagicArrowClientAnimations {
    private static final int BOW_PULL_TICKS = 20;

    private static final ResourceLocation BOW_PULL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "pulling_bow"
            );
    private static final ResourceLocation BOW_IDLE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "pulling_bow_idle"
            );
    private static final ResourceLocation BOW_CROUCH_PULL =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "pulling_bow_crouch"
            );
    private static final ResourceLocation BOW_CROUCH_IDLE =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "pulling_bow_crouch_idle"
            );
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

    private static final Map<UUID, PlaybackState> PLAYING =
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

        UUID id = player.getUUID();
        PlaybackState current = PLAYING.get(id);
        int castStartTick = current == null
                ? player.tickCount
                : current.castStartTick();

        HumanoidArm crossbowArm =
                MagicArrowClientVisuals.getCrossbowArm(player);

        if (crossbowArm != null) {
            Pose pose = crossbowArm == HumanoidArm.RIGHT
                    ? Pose.CROSSBOW_RIGHT
                    : Pose.CROSSBOW_LEFT;
            play(player, pose, castStartTick);
            return;
        }

        if (isHoldingBow(player)) {
            int elapsedTicks = Math.max(
                    0,
                    player.tickCount - castStartTick
            );
            boolean settled = elapsedTicks >= BOW_PULL_TICKS;
            boolean crouching = player.isCrouching();

            Pose pose;
            if (crouching) {
                pose = settled
                        ? Pose.BOW_CROUCH_IDLE
                        : Pose.BOW_CROUCH_PULL;
            } else {
                pose = settled
                        ? Pose.BOW_IDLE
                        : Pose.BOW_PULL;
            }

            play(player, pose, castStartTick);
            return;
        }

        /*
         * This can only normally happen if equipment is changed after
         * the server accepted the cast. Do not leave a stale pose active.
         */
        stop(player);
    }

    private static boolean isHoldingBow(Player player) {
        return RangedWeaponClassifier.isBow(
                player.getMainHandItem()
        ) || RangedWeaponClassifier.isBow(
                player.getOffhandItem()
        );
    }

    @SuppressWarnings("unchecked")
    private static void play(
            AbstractClientPlayer player,
            Pose pose,
            int castStartTick
    ) {
        UUID id = player.getUUID();
        PlaybackState current = PLAYING.get(id);

        if (current != null && current.pose() == pose) {
            return;
        }

        ModifierLayer<IAnimation> layer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess
                        .getPlayerAssociatedData(player)
                        .get(MagicArrowRangedAnimationLayer.LAYER_ID);

        if (layer == null) {
            return;
        }

        ResourceLocation animationId = pose.animationId();
        var animation =
                PlayerAnimationRegistry.getAnimation(animationId);

        if (animation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Magic Arrow ranged animation {}",
                    animationId
            );
            return;
        }

        layer.setAnimation(new KeyframeAnimationPlayer(animation));
        PLAYING.put(
                id,
                new PlaybackState(castStartTick, pose)
        );
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

    private record PlaybackState(
            int castStartTick,
            Pose pose
    ) {
    }

    private enum Pose {
        BOW_PULL(MagicArrowClientAnimations.BOW_PULL),
        BOW_IDLE(MagicArrowClientAnimations.BOW_IDLE),
        BOW_CROUCH_PULL(MagicArrowClientAnimations.BOW_CROUCH_PULL),
        BOW_CROUCH_IDLE(MagicArrowClientAnimations.BOW_CROUCH_IDLE),
        CROSSBOW_RIGHT(MagicArrowClientAnimations.CROSSBOW_HOLD_RIGHT),
        CROSSBOW_LEFT(MagicArrowClientAnimations.CROSSBOW_HOLD_LEFT);

        private final ResourceLocation animationId;

        Pose(ResourceLocation animationId) {
            this.animationId = animationId;
        }

        private ResourceLocation animationId() {
            return animationId;
        }
    }
}
