package com.w0of26.martialspells.client.animation;

import com.w0of26.martialspells.MartialSpells;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.api.layered.modifier.MirrorModifier;
import dev.kosmx.playerAnim.api.layered.modifier.SpeedModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class FlurryClientAnimations {
    /*
     * The copied Better Combat one-handed punch animation.
     */
    private static final ResourceLocation PUNCH_ANIMATION =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "flurry_of_blows"
            );

    /*
     * Each punch animation begins three ticks before its matching
     * server-side damage tick.
     *
     * Animation starts: 0, 4, 8, 12, 16, 20
     * Server contacts:  3, 7, 11, 15, 19, 23
     */
    private static final int[][] PUNCH_START_TICKS = {
            {0, 4},
            {0, 4, 8},
            {0, 4, 8, 12},
            {0, 4, 8, 12, 16},
            {0, 4, 8, 12, 16, 20}
    };

    /*
     * Each source punch is compressed into roughly four ticks so
     * that the next alternating punch can begin immediately.
     */
    private static final float PUNCH_DURATION_TICKS =
            4.0F;

    private static final Map<UUID, ActiveSequence>
            ACTIVE_SEQUENCES = new HashMap<>();

    private FlurryClientAnimations() {
    }

    /**
     * Starts the visual Flurry sequence when the wind-up completes.
     */
    public static void play(
            LivingEntity entity,
            int spellLevel
    ) {
        if (!(entity
                instanceof AbstractClientPlayer player)) {
            return;
        }

        int levelIndex =
                clampLevel(spellLevel) - 1;

        ActiveSequence sequence =
                new ActiveSequence(
                        player,
                        player.tickCount,
                        levelIndex
                );

        ACTIVE_SEQUENCES.put(
                player.getUUID(),
                sequence
        );

        /*
         * Start the first punch immediately instead of waiting until
         * the next client tick.
         */
        advanceSequence(sequence);
    }

    @SubscribeEvent
    public static void onClientTick(
            TickEvent.ClientTickEvent event
    ) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.level == null) {
            ACTIVE_SEQUENCES.clear();
            return;
        }

        Iterator<Map.Entry<UUID, ActiveSequence>>
                iterator =
                ACTIVE_SEQUENCES
                        .entrySet()
                        .iterator();

        while (iterator.hasNext()) {
            ActiveSequence sequence =
                    iterator.next().getValue();

            AbstractClientPlayer player =
                    sequence.player;

            /*
             * Remove stale sequences after death, disconnection,
             * world replacement, or dimension travel.
             */
            if (!player.isAlive()
                    || player.isRemoved()
                    || player.level()
                    != minecraft.level) {
                iterator.remove();
                continue;
            }

            advanceSequence(sequence);

            int[] schedule =
                    PUNCH_START_TICKS[
                            sequence.levelIndex
                            ];

            if (sequence.nextPunchIndex
                    >= schedule.length) {
                iterator.remove();
            }
        }
    }

    private static void advanceSequence(
            ActiveSequence sequence
    ) {
        int[] schedule =
                PUNCH_START_TICKS[
                        sequence.levelIndex
                        ];

        int elapsedTicks =
                sequence.player.tickCount
                        - sequence.startTick;

        while (sequence.nextPunchIndex
                < schedule.length
                && elapsedTicks
                >= schedule[
                sequence.nextPunchIndex
                ]) {

            boolean offhandPunch =
                    sequence.nextPunchIndex % 2 == 1;

            playPunch(
                    sequence.player,
                    offhandPunch
            );

            sequence.nextPunchIndex++;
        }
    }

    private static void playPunch(
            AbstractClientPlayer player,
            boolean offhandPunch
    ) {
        var keyframeAnimation =
                PlayerAnimationRegistry.getAnimation(
                        PUNCH_ANIMATION
                );

        if (keyframeAnimation == null) {
            MartialSpells.LOGGER.warn(
                    "Could not find Flurry punch animation {}",
                    PUNCH_ANIMATION
            );
            return;
        }

        @SuppressWarnings("unchecked")
        ModifierLayer<IAnimation> castingLayer =
                (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess
                                .getPlayerAssociatedData(
                                        player
                                )
                                .get(
                                        SpellAnimations
                                                .ANIMATION_RESOURCE
                                );

        if (castingLayer == null) {
            MartialSpells.LOGGER.warn(
                    "Iron's casting animation layer was not "
                            + "available for {}",
                    player.getGameProfile().getName()
            );
            return;
        }

        /*
         * Better Combat mirrors an offhand attack. Invert that choice
         * for players whose configured main arm is left.
         */
        boolean mirrored = offhandPunch;

        if (player.getMainArm()
                == HumanoidArm.LEFT) {
            mirrored = !mirrored;
        }

        KeyframeAnimationPlayer animationPlayer =
                new KeyframeAnimationPlayer(
                        keyframeAnimation
                );

        /*
         * Better Combat also scales an animation according to the
         * desired attack duration. Compress the copied animation into
         * the four-tick interval between Flurry strikes.
         */
        float animationSpeed =
                Math.max(
                        1.0F,
                        keyframeAnimation.endTick
                                / PUNCH_DURATION_TICKS
                );

        ModifierLayer<IAnimation> punchLayer =
                new ModifierLayer<>(
                        animationPlayer
                );

        punchLayer.addModifierBefore(
                new SpeedModifier(
                        animationSpeed
                )
        );

        punchLayer.addModifierBefore(
                new MirrorModifier(
                        mirrored
                )
        );

        castingLayer.replaceAnimationWithFade(
                AbstractFadeModifier.standardFadeIn(
                        1,
                        Ease.INOUTSINE
                ),
                punchLayer,
                true
        );
    }

    private static int clampLevel(
            int spellLevel
    ) {
        return Math.max(
                1,
                Math.min(
                        spellLevel,
                        PUNCH_START_TICKS.length
                )
        );
    }

    private static final class ActiveSequence {
        private final AbstractClientPlayer player;
        private final int startTick;
        private final int levelIndex;

        private int nextPunchIndex;

        private ActiveSequence(
                AbstractClientPlayer player,
                int startTick,
                int levelIndex
        ) {
            this.player = player;
            this.startTick = startTick;
            this.levelIndex = levelIndex;
            this.nextPunchIndex = 0;
        }
    }
}