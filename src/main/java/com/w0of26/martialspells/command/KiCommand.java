package com.w0of26.martialspells.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.w0of26.martialspells.MartialSpells;
import com.w0of26.martialspells.ki.KiHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Development commands for inspecting and modifying player Ki.
 *
 * These commands require permission level 2.
 */
@Mod.EventBusSubscriber(
        modid = MartialSpells.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class KiCommand {
    private KiCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(
            RegisterCommandsEvent event
    ) {
        event.getDispatcher().register(
                Commands.literal("martialspells")
                        .requires(
                                source ->
                                        source.hasPermission(2)
                        )
                        .then(
                                Commands.literal("ki")
                                        .then(createGetCommand())
                                        .then(createSetCommand())
                                        .then(createSetMaximumCommand())
                                        .then(createAddCommand())
                                        .then(createRemoveCommand())
                                        .then(createFillCommand())
                                        .then(createResetCommand())
                        )
        );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createGetCommand() {
        return Commands.literal("get")
                .then(
                        Commands.argument(
                                        "target",
                                        EntityArgument.player()
                                )
                                .executes(context ->
                                        getKi(
                                                context.getSource(),
                                                EntityArgument.getPlayer(
                                                        context,
                                                        "target"
                                                )
                                        )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createSetCommand() {
        return Commands.literal("set")
                .then(
                        Commands.argument(
                                        "target",
                                        EntityArgument.player()
                                )
                                .then(
                                        Commands.argument(
                                                        "amount",
                                                        IntegerArgumentType
                                                                .integer(0)
                                                )
                                                .executes(context ->
                                                        setKi(
                                                                context.getSource(),
                                                                EntityArgument
                                                                        .getPlayer(
                                                                                context,
                                                                                "target"
                                                                        ),
                                                                IntegerArgumentType
                                                                        .getInteger(
                                                                                context,
                                                                                "amount"
                                                                        )
                                                        )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createSetMaximumCommand() {
        return Commands.literal("setmax")
                .then(
                        Commands.argument(
                                        "target",
                                        EntityArgument.player()
                                )
                                .then(
                                        Commands.argument(
                                                        "amount",
                                                        IntegerArgumentType
                                                                .integer(0)
                                                )
                                                .executes(context ->
                                                        setMaximumKi(
                                                                context.getSource(),
                                                                EntityArgument
                                                                        .getPlayer(
                                                                                context,
                                                                                "target"
                                                                        ),
                                                                IntegerArgumentType
                                                                        .getInteger(
                                                                                context,
                                                                                "amount"
                                                                        )
                                                        )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createAddCommand() {
        return Commands.literal("add")
                .then(
                        Commands.argument(
                                        "target",
                                        EntityArgument.player()
                                )
                                .then(
                                        Commands.argument(
                                                        "amount",
                                                        IntegerArgumentType
                                                                .integer(1)
                                                )
                                                .executes(context ->
                                                        addKi(
                                                                context.getSource(),
                                                                EntityArgument
                                                                        .getPlayer(
                                                                                context,
                                                                                "target"
                                                                        ),
                                                                IntegerArgumentType
                                                                        .getInteger(
                                                                                context,
                                                                                "amount"
                                                                        )
                                                        )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createRemoveCommand() {
        return Commands.literal("remove")
                .then(
                        Commands.argument(
                                        "target",
                                        EntityArgument.player()
                                )
                                .then(
                                        Commands.argument(
                                                        "amount",
                                                        IntegerArgumentType
                                                                .integer(1)
                                                )
                                                .executes(context ->
                                                        removeKi(
                                                                context.getSource(),
                                                                EntityArgument
                                                                        .getPlayer(
                                                                                context,
                                                                                "target"
                                                                        ),
                                                                IntegerArgumentType
                                                                        .getInteger(
                                                                                context,
                                                                                "amount"
                                                                        )
                                                        )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createFillCommand() {
        return Commands.literal("fill")
                .then(
                        Commands.argument(
                                        "target",
                                        EntityArgument.player()
                                )
                                .executes(context ->
                                        fillKi(
                                                context.getSource(),
                                                EntityArgument.getPlayer(
                                                        context,
                                                        "target"
                                                )
                                        )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .LiteralArgumentBuilder<CommandSourceStack>
    createResetCommand() {
        return Commands.literal("reset")
                .then(
                        Commands.argument(
                                        "target",
                                        EntityArgument.player()
                                )
                                .executes(context ->
                                        resetKi(
                                                context.getSource(),
                                                EntityArgument.getPlayer(
                                                        context,
                                                        "target"
                                                )
                                        )
                                )
                );
    }

    private static int getKi(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        sendKiStatus(
                source,
                player
        );

        return KiHelper.getCurrentKi(player);
    }

    private static int setKi(
            CommandSourceStack source,
            ServerPlayer player,
            int amount
    ) {
        KiHelper.setCurrentKi(
                player,
                amount
        );

        sendKiStatus(
                source,
                player
        );

        return KiHelper.getCurrentKi(player);
    }

    private static int setMaximumKi(
            CommandSourceStack source,
            ServerPlayer player,
            int amount
    ) {
        KiHelper.setMaximumKi(
                player,
                amount
        );

        sendKiStatus(
                source,
                player
        );

        return KiHelper.getMaximumKi(player);
    }

    private static int addKi(
            CommandSourceStack source,
            ServerPlayer player,
            int amount
    ) {
        int added =
                KiHelper.addKi(
                        player,
                        amount
                );

        source.sendSuccess(
                () -> Component.literal(
                        "Added "
                                + added
                                + " Ki to "
                                + player.getGameProfile().getName()
                                + "."
                ),
                false
        );

        sendKiStatus(
                source,
                player
        );

        return added;
    }

    private static int removeKi(
            CommandSourceStack source,
            ServerPlayer player,
            int amount
    ) {
        int removed =
                KiHelper.removeKi(
                        player,
                        amount
                );

        source.sendSuccess(
                () -> Component.literal(
                        "Removed "
                                + removed
                                + " Ki from "
                                + player.getGameProfile().getName()
                                + "."
                ),
                false
        );

        sendKiStatus(
                source,
                player
        );

        return removed;
    }

    private static int fillKi(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        KiHelper.fillKi(player);

        sendKiStatus(
                source,
                player
        );

        return KiHelper.getCurrentKi(player);
    }

    private static int resetKi(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        KiHelper.resetKi(player);

        sendKiStatus(
                source,
                player
        );

        return 1;
    }

    private static void sendKiStatus(
            CommandSourceStack source,
            ServerPlayer player
    ) {
        int currentKi =
                KiHelper.getCurrentKi(player);

        int maximumKi =
                KiHelper.getMaximumKi(player);

        source.sendSuccess(
                () -> Component.literal(
                        player.getGameProfile().getName()
                                + " Ki: "
                                + currentKi
                                + "/"
                                + maximumKi
                ),
                false
        );
    }
}