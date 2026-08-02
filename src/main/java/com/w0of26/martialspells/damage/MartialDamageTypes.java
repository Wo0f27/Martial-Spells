package com.w0of26.martialspells.damage;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class MartialDamageTypes {
    public static final ResourceKey<DamageType>
            GUARDIANS_COVENANT_REDIRECT =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "guardians_covenant_redirect"
                    )
            );

    public static final ResourceKey<DamageType>
            FLURRY_OF_BLOWS =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "flurry_of_blows"
                    )
            );


    private MartialDamageTypes() {
    }

    /**
     * Creates an unattributed Covenant redirect source.
     * <p>
     * Leaving the causing and direct entities unset avoids treating
     * the tank as attacking itself and avoids source-position-based
     * knockback behavior.
     */
    public static DamageSource guardiansCovenantRedirect(
            Level level
    ) {
        Holder<DamageType> damageType =
                level.registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                GUARDIANS_COVENANT_REDIRECT
                        );

        return new DamageSource(damageType);
    }


    public static DamageSource flurryOfBlows(
            ServerPlayer player
    ) {
        Holder<DamageType> damageType =
                player.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                FLURRY_OF_BLOWS
                        );

        return new DamageSource(
                damageType,
                player,
                player
        );
    }
    /**
     * Used by the future LivingHurtEvent handler to ensure damage
     * redirected into the tank can never be redirected again.
     */
    public static boolean isGuardiansCovenantRedirect(
            DamageSource source
    ) {
        return source.is(
                GUARDIANS_COVENANT_REDIRECT
        );
    }

}