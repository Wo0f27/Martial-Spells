package com.w0of26.martialspells.damage;

import com.w0of26.martialspells.MartialSpells;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

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

    public static final ResourceKey<DamageType>
            STUNNING_STRIKE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "stunning_strike"
                    )
            );

    public static final ResourceKey<DamageType>
            HEAVENFALL_STRIKE =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "heavenfall_strike"
                    )
            );

    public static final ResourceKey<DamageType>
            CALTROPS =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "caltrops"
                    )
            );

    public static final ResourceKey<DamageType>
            BEAR_TRAP =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "bear_trap"
                    )
            );

    public static final ResourceKey<DamageType>
            DEMORALIZING_SHOUT =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "demoralizing_shout"
                    )
            );

    public static final ResourceKey<DamageType>
            THROW_NET =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "throw_net"
                    )
            );

    public static final ResourceKey<DamageType>
            SHATTERING_THROW =
            ResourceKey.create(
                    Registries.DAMAGE_TYPE,
                    new ResourceLocation(
                            MartialSpells.MOD_ID,
                            "shattering_throw"
                    )
            );

    private MartialDamageTypes() {
    }

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

    public static boolean isGuardiansCovenantRedirect(
            DamageSource source
    ) {
        return source.is(
                GUARDIANS_COVENANT_REDIRECT
        );
    }

    public static DamageSource stunningStrike(
            ServerPlayer player
    ) {
        Holder<DamageType> damageType =
                player.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                STUNNING_STRIKE
                        );

        return new DamageSource(
                damageType,
                player,
                player
        );
    }

    public static DamageSource heavenfallStrike(
            ServerPlayer player
    ) {
        Holder<DamageType> damageType =
                player.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(
                                HEAVENFALL_STRIKE
                        );

        return new DamageSource(
                damageType,
                player,
                player
        );
    }

    public static DamageSource caltrops(
            ServerPlayer player,
            Entity directEntity
    ) {
        Holder<DamageType> damageType =
                player.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(CALTROPS);

        return new DamageSource(
                damageType,
                directEntity,
                player
        );
    }

    public static DamageSource bearTrap(
            ServerPlayer player,
            Entity directEntity
    ) {
        Holder<DamageType> damageType =
                player.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(BEAR_TRAP);

        return new DamageSource(
                damageType,
                directEntity,
                player
        );
    }

    public static DamageSource demoralizingShout(
            net.minecraft.world.entity.LivingEntity caster
    ) {
        Holder<DamageType> damageType =
                caster.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(DEMORALIZING_SHOUT);

        return new DamageSource(
                damageType,
                caster,
                caster
        );
    }

    public static DamageSource throwNet(
            net.minecraft.world.entity.LivingEntity caster,
            Entity directEntity
    ) {
        Holder<DamageType> damageType =
                caster.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(THROW_NET);

        return new DamageSource(
                damageType,
                directEntity,
                caster
        );
    }

    public static DamageSource shatteringThrow(
            net.minecraft.world.entity.LivingEntity caster,
            Entity directEntity
    ) {
        Holder<DamageType> damageType =
                caster.level()
                        .registryAccess()
                        .registryOrThrow(
                                Registries.DAMAGE_TYPE
                        )
                        .getHolderOrThrow(SHATTERING_THROW);

        return new DamageSource(
                damageType,
                directEntity,
                caster
        );
    }
}
