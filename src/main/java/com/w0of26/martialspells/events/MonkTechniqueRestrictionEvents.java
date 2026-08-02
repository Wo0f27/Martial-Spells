package com.w0of26.martialspells.events;

import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.item.MonkCodexItem;
import com.w0of26.martialspells.registry.MartialSpellRegistry;
import io.redspace.ironsspellbooks.api.events.InscribeSpellEvent;
import io.redspace.ironsspellbooks.gui.inscription_table.InscriptionTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Prevents Monk techniques from being inscribed into ordinary
 * spellbooks.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MonkTechniqueRestrictionEvents {
    private MonkTechniqueRestrictionEvents() {
    }

    @SubscribeEvent
    public static void onInscribeSpell(
            InscribeSpellEvent event
    ) {
        /*
         * Ignore ordinary Iron's spells and non-Monk spells.
         */
        if (!event.getSpellData()
                .getSpell()
                .equals(
                        MartialSpellRegistry
                                .STILLWATER_MEDITATION
                                .get()
                )) {
            return;
        }

        /*
         * Iron's fires the event from its Inscription Table menu.
         * Fail safely if another system somehow fires the event
         * without an accessible target spellbook.
         */
        if (!(event.getEntity().containerMenu
                instanceof InscriptionTableMenu menu)) {
            event.setCanceled(true);
            return;
        }

        ItemStack targetSpellbook =
                menu.getSpellBookSlot().getItem();

        /*
         * Monk techniques may only enter a Monk Codex.
         */
        if (!(targetSpellbook.getItem()
                instanceof MonkCodexItem)) {
            event.setCanceled(true);

            if (event.getEntity()
                    instanceof ServerPlayer player) {
                player.displayClientMessage(
                        Component.translatable(
                                "ui.martial_spells.monk_codex_only"
                        ).withStyle(ChatFormatting.RED),
                        true
                );
            }
        }
    }
}