package com.w0of26.martialspells.spells;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.registry.MartialSchoolRegistry;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class DeflectMissilesSpell
        extends AbstractMonkTechniqueSpell {

    public static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(
                    MartialSpells.MOD_ID,
                    "deflect_missiles"
            );

    private static final int MAX_LEVEL = 5;

    /*
     * TEST VALUE ONLY.
     *
     * 100 ticks = 5 seconds maximum channel.
     * Final duration is still undecided.
     */
    private static final int TEST_CHANNEL_TICKS = 100;

    private final DefaultConfig defaultConfig =
            new DefaultConfig()
                    .setSchoolResource(
                            MartialSchoolRegistry
                                    .MARTIAL_RESOURCE
                    )
                    /*
                     * Provisional rarity.
                     * Final Deflect Missiles rarity is still open.
                     */
                    .setMinRarity(
                            SpellRarity.RARE
                    )
                    .setMaxLevel(
                            MAX_LEVEL
                    )
                    /*
                     * No meaningful cooldown during lifecycle testing.
                     */
                    .setCooldownSeconds(
                            0
                    )
                    .build();

    public DeflectMissilesSpell() {
        super(MAX_LEVEL);

        /*
         * Continuous spells remain in their casting state
         * while the player holds the cast.
         */
        this.castTime = TEST_CHANNEL_TICKS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public CastResult canBeCastedBy(
            int spellLevel,
            CastSource castSource,
            MagicData magicData,
            Player player
    ) {
        CastResult normalResult =
                super.canBeCastedBy(
                        spellLevel,
                        castSource,
                        magicData,
                        player
                );

        if (!normalResult.isSuccess()) {
            return normalResult;
        }

        CastResult sourceResult =
                validateMonkTechniqueSource(
                        castSource,
                        player
                );

        if (!sourceResult.isSuccess()) {
            return sourceResult;
        }

        /*
         * No Ki validation yet.
         *
         * Ki behavior is intentionally deferred until the
         * continuous-cast lifecycle is proven.
         */
        return success();
    }
}