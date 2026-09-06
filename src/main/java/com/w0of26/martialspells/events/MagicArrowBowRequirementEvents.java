package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Makes Iron's base Magic Arrow behave like a ranged weapon technique
 * while Martial Spells is installed.
 *
 * The base spell itself is left untouched; this only vetoes player
 * casts that begin without a recognized bow or crossbow held in either
 * hand. Recognition is datapack-extensible through Martial Spells item
 * tags, with normal BowItem/CrossbowItem inheritance as a fallback.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MagicArrowBowRequirementEvents {
    private static final String MAGIC_ARROW_ID =
            "irons_spellbooks:magic_arrow";

    private MagicArrowBowRequirementEvents() {
    }

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (!MAGIC_ARROW_ID.equals(event.getSpellId())) {
            return;
        }

        Player player = event.getEntity();
        if (isHoldingValidRangedWeapon(player)) {
            return;
        }

        event.setCanceled(true);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Magic Arrow requires a bow or crossbow in either hand."
                    ).withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    private static boolean isHoldingValidRangedWeapon(Player player) {
        return RangedWeaponClassifier.isSupported(
                player.getMainHandItem()
        ) || RangedWeaponClassifier.isSupported(
                player.getOffhandItem()
        );
    }
}
