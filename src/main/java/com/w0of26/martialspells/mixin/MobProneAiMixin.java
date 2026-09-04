package com.w0of26.martialspells.mixin;

import com.w0of26.martialspells.prone.ProneHelper;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobProneAiMixin {

    @Inject(
            method = "serverAiStep",
            at = @At("HEAD"),
            cancellable = true
    )
    private void martialSpells$stopProneAi(
            CallbackInfo ci
    ) {
        Mob mob =
                (Mob) (Object) this;

        if (!ProneHelper.isProne(
                mob
        )) {
            return;
        }

        mob.getNavigation()
                .stop();

        ci.cancel();
    }
}