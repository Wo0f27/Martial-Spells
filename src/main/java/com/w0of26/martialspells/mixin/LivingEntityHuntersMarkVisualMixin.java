package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.effects.HuntersMarkVisualState;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla does not broadcast each mob's effect list to all observers. Spell
 * Engine tracks synchronized effects separately for its model renderers; this
 * single boolean supplies only the Hunter's Mark visual to tracking clients.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityHuntersMarkVisualMixin implements HuntersMarkVisualState {
    @Unique
    private static final EntityDataAccessor<Boolean> MARTIAL_SPELLS_HUNTERS_MARK_VISUAL =
            SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void martialSpells$defineHuntersMarkVisual(CallbackInfo ci) {
        martialSpells$self().getEntityData().define(MARTIAL_SPELLS_HUNTERS_MARK_VISUAL, false);
    }

    @Inject(method = "onEffectAdded", at = @At("TAIL"))
    private void martialSpells$onHuntersMarkAdded(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (effect.getEffect() == MartialEffectRegistry.HUNTERS_MARK.get()) {
            martialSpells$syncHuntersMarkVisual();
        }
    }

    @Inject(method = "onEffectUpdated", at = @At("TAIL"))
    private void martialSpells$onHuntersMarkUpdated(MobEffectInstance effect, boolean reapplyAttributes,
                                                     Entity source, CallbackInfo ci) {
        if (effect.getEffect() == MartialEffectRegistry.HUNTERS_MARK.get()) {
            martialSpells$syncHuntersMarkVisual();
        }
    }

    @Inject(method = "onEffectRemoved", at = @At("TAIL"))
    private void martialSpells$onHuntersMarkRemoved(MobEffectInstance effect, CallbackInfo ci) {
        if (effect.getEffect() == MartialEffectRegistry.HUNTERS_MARK.get()) {
            martialSpells$syncHuntersMarkVisual();
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void martialSpells$restoreHuntersMarkVisual(CompoundTag tag, CallbackInfo ci) {
        martialSpells$syncHuntersMarkVisual();
    }

    @Unique
    private void martialSpells$syncHuntersMarkVisual() {
        LivingEntity self = martialSpells$self();
        if (!self.level().isClientSide) {
            self.getEntityData().set(MARTIAL_SPELLS_HUNTERS_MARK_VISUAL,
                    self.hasEffect(MartialEffectRegistry.HUNTERS_MARK.get()));
        }
    }

    @Unique
    private LivingEntity martialSpells$self() {
        return (LivingEntity) (Object) this;
    }

    @Override
    public boolean martialSpells$hasHuntersMarkVisual() {
        return martialSpells$self().getEntityData().get(MARTIAL_SPELLS_HUNTERS_MARK_VISUAL);
    }
}
