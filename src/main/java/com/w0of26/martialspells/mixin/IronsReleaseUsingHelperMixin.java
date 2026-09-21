package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.technique.ReleaseChargedTechnique;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Converts only opted-in Martial charged techniques from Iron's early-release
 * cancellation into source-faithful charged release. All normal Iron's spells
 * keep the unmodified releaseUsingHelper path.
 */
@Mixin(value = Utils.class, remap = false)
public abstract class IronsReleaseUsingHelperMixin {
    @Inject(method = "releaseUsingHelper", at = @At("HEAD"), cancellable = true)
    private static void martialSpells$releaseChargedTechnique(
            LivingEntity entity,
            ItemStack itemStack,
            int ticksUsed,
            CallbackInfo ci
    ) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        MagicData magicData = MagicData.getPlayerMagicData(player);
        if (!magicData.isCasting()) {
            return;
        }

        if (magicData.getCastingSpell().getSpell()
                instanceof ReleaseChargedTechnique chargedTechnique
                && chargedTechnique.releaseOnUseStop(player, magicData)) {
            ci.cancel();
        }
    }
}
