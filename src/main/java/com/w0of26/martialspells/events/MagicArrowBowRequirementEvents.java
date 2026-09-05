package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Makes Iron's base Magic Arrow behave like a bow technique while
 * Martial Spells is installed.
 *
 * The base spell itself is left untouched; this only vetoes player
 * casts that begin without a bow held in either hand. Because this
 * runs through Iron's SpellPreCastEvent, the rule applies consistently
 * to scroll, spellbook, and other player cast sources.
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
        if (isHoldingBow(player)) {
            return;
        }

        event.setCanceled(true);

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(
                    Component.literal(
                            "Magic Arrow requires a bow in either hand."
                    ).withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    private static boolean isHoldingBow(Player player) {
        return player.getMainHandItem().getItem() instanceof BowItem
                || player.getOffhandItem().getItem() instanceof BowItem;
    }
}
