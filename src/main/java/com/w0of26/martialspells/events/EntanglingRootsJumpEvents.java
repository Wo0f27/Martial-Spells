package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Attribute-less living entities get half of the vanilla 0.42 base jump removed. */
@Mod.EventBusSubscriber(modid = MartialSpells.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EntanglingRootsJumpEvents {
    private static final double HALF_BASE_JUMP = 0.21D;

    private EntanglingRootsJumpEvents() {}

    @SubscribeEvent
    public static void onJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(MartialEffectRegistry.ENTANGLING_ROOTS.get())
                || entity.getAttribute(Attributes.JUMP_STRENGTH) != null) return;
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x, Math.max(0.0D, motion.y - HALF_BASE_JUMP), motion.z);
    }
}
