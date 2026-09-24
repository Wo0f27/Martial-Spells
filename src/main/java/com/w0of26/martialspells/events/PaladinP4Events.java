package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialEffectRegistry;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import com.w0of26.martialspells.spells.PaladinLightwellSpell;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.datagen.DamageTypeTagGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/** Cross-system integration hooks required by frozen Paladins P4 mechanics. */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class PaladinP4Events {
    private static final int LIGHTWELL_FIXED_COOLDOWN_TICKS =
            PaladinLightwellSpell.BASE_COOLDOWN_SECONDS * 20;

    private static final TagKey<DamageType> COMMON_MAGIC =
            TagKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            "c",
                            "is_magic"
                    )
            );

    private static final TagKey<DamageType> FORGE_MAGIC =
            TagKey.create(
                    Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath(
                            "forge",
                            "is_magic"
                    )
            );

    private static final ResourceLocation RANGED_HASTE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "ranged_weapon",
                    "haste"
            );

    private static final UUID RANGED_HASTE_MODIFIER_ID =
            UUID.fromString(
                    "8a6eef4f-6de9-4f0a-bf3c-cb39a4ff8b05"
            );

    private PaladinP4Events() {
    }

    @SubscribeEvent
    public static void onProtectedAttack(
            LivingAttackEvent event
    ) {
        LivingEntity target =
                event.getEntity();

        if (!target.hasEffect(
                MartialEffectRegistry
                        .BARRIER_PROTECTED
                        .get()
        )) {
            return;
        }

        var source =
                event.getSource();

        if (source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(COMMON_MAGIC)
                || source.is(FORGE_MAGIC)
                || source.is(DamageTypeTagGenerator.FIRE_MAGIC)
                || source.is(DamageTypeTagGenerator.ICE_MAGIC)
                || source.is(DamageTypeTagGenerator.LIGHTNING_MAGIC)
                || source.is(DamageTypeTagGenerator.HOLY_MAGIC)
                || source.is(DamageTypeTagGenerator.ENDER_MAGIC)
                || source.is(DamageTypeTagGenerator.BLOOD_MAGIC)
                || source.is(DamageTypeTagGenerator.EVOCATION_MAGIC)
                || source.is(DamageTypeTagGenerator.ELDRITCH_MAGIC)
                || source.is(DamageTypeTagGenerator.NATURE_MAGIC)) {
            event.setCanceled(true);
        }
    }

    /**
     * Frozen Lightwell explicitly disables haste on its 45-second summon
     * cooldown. Iron's computes cooldown reduction before the Pre event, so
     * force the source-authored fixed duration back here.
     */
    @SubscribeEvent
    public static void onSpellCooldownAdded(
            SpellCooldownAddedEvent.Pre event
    ) {
        if (event.getSpell()
                == MartialSpellRegistry.LIGHTWELL.get()) {
            event.setEffectiveCooldown(
                    LIGHTWELL_FIXED_COOLDOWN_TICKS
            );
        }
    }

    /**
     * RangedWeaponAPI integration without a hard dependency. Upstream Battle
     * Banner grants +40% ranged haste in addition to melee/spell haste.
     */
    @SubscribeEvent
    public static void onLivingTick(
            LivingEvent.LivingTickEvent event
    ) {
        LivingEntity living =
                event.getEntity();

        if (living.level().isClientSide) {
            return;
        }

        Attribute rangedHaste =
                ForgeRegistries.ATTRIBUTES.getValue(
                        RANGED_HASTE_ID
                );
        if (rangedHaste == null) {
            return;
        }

        AttributeInstance instance =
                living.getAttribute(
                        rangedHaste
                );
        if (instance == null) {
            return;
        }

        boolean active =
                living.hasEffect(
                        MartialEffectRegistry
                                .BATTLE_BANNER
                                .get()
                );

        AttributeModifier existing =
                instance.getModifier(
                        RANGED_HASTE_MODIFIER_ID
                );

        if (active) {
            if (existing == null) {
                instance.addTransientModifier(
                        new AttributeModifier(
                                RANGED_HASTE_MODIFIER_ID,
                                MartialSpells.MOD_ID
                                        + ".battle_banner_ranged_haste",
                                0.40D,
                                AttributeModifier.Operation.MULTIPLY_BASE
                        )
                );
            }
        } else if (existing != null) {
            instance.removeModifier(
                    RANGED_HASTE_MODIFIER_ID
            );
        }
    }
}
