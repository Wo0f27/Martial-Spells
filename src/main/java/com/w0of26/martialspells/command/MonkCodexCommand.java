package com.w0of26.martialspells.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.item.MonkCodexItem;
import com.w0of26.martialspells.item.MonkCodexTier;
import com.w0of26.martialspells.item.MonkCodexUpgradeHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Development commands for testing Monk Codex progression.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class MonkCodexCommand {
    private MonkCodexCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal(
                                "martialspells"
                        )
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )
                        .then(
                                Commands.literal(
                                                "codex"
                                        )
                                        .then(
                                                Commands.literal(
                                                                "upgrade"
                                                        )
                                                        .executes(
                                                                context ->
                                                                        upgradeHeldCodex(
                                                                                context.getSource()
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int upgradeHeldCodex(
            CommandSourceStack source
    ) throws CommandSyntaxException {
        ServerPlayer player =
                source.getPlayerOrException();

        ItemStack heldStack =
                player.getMainHandItem();

        MonkCodexUpgradeHelper.UpgradeResult result =
                MonkCodexUpgradeHelper
                        .upgradeToNextTier(
                                heldStack
                        );

        if (result
                == MonkCodexUpgradeHelper
                .UpgradeResult.SUCCESS) {

            MonkCodexTier newTier =
                    MonkCodexItem.getTier(
                            heldStack
                    );

            player.getInventory().setChanged();
            player.inventoryMenu
                    .broadcastChanges();

            source.sendSuccess(
                    () -> Component.literal(
                            "Upgraded Monk Codex to Tier "
                                    + newTier
                                    .getDisplayName()
                                    + "."
                    ),
                    false
            );

            return 1;
        }

        source.sendFailure(
                Component.literal(
                        getFailureMessage(result)
                )
        );

        return 0;
    }

    private static String getFailureMessage(
            MonkCodexUpgradeHelper
                    .UpgradeResult result
    ) {
        return switch (result) {
            case INVALID_ITEM ->
                    "Hold a Monk Codex in your main hand.";

            case ALREADY_MAXIMUM ->
                    "This Monk Codex is already Tier V.";

            case MISSING_CONTAINER ->
                    "The Monk Codex has no valid spell container.";

            case CORE_INSERT_FAILED ->
                    "The Codex core techniques could not be rebuilt.";

            case OPEN_SPELL_COPY_FAILED ->
                    "An existing spell could not be preserved. "
                            + "The Codex was not upgraded.";

            case SUCCESS ->
                    "The Monk Codex was upgraded.";
        };
    }
}