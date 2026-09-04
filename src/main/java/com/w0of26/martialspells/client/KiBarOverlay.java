package com.w0of26.martialspells.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import com.w0of26.martialspells.registry.MartialEffectRegistry;

/**
 * Displays the local player's synchronized Ki above Iron's mana bar.
 */
public final class KiBarOverlay implements IGuiOverlay {
    public static final KiBarOverlay INSTANCE =
            new KiBarOverlay();

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    "irons_spellbooks",
                    "textures/gui/icons.png"
            );

    /*
     * Iron's normal Hunger/right-side mana-bar sprite.
     */
    private static final int BAR_WIDTH = 98;
    private static final int BAR_HEIGHT = 21;

    private static final int SPRITE_X = 0;
    private static final int EMPTY_SPRITE_Y = 0;
    private static final int FILLED_SPRITE_Y =
            EMPTY_SPRITE_Y + BAR_HEIGHT;

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 256;

    /*
     * The normal rightHeight increment is only 10 pixels.
     * This extra amount prevents the 21-pixel bars from visually
     * overlapping.
     */
    private static final int EXTRA_VERTICAL_SEPARATION = 12;

    private static final int TEXT_COLOR = 0x7FFFA8;

    private static final float TINT_RED = 0.35F;
    private static final float TINT_GREEN = 1.00F;
    private static final float TINT_BLUE = 0.50F;

    private KiBarOverlay() {
    }

    @Override
    public void render(
            ForgeGui gui,
            GuiGraphics graphics,
            float partialTick,
            int screenWidth,
            int screenHeight
    ) {
        Minecraft minecraft =
                Minecraft.getInstance();

        if (minecraft.player == null
                || minecraft.player.isSpectator()) {
            return;
        }

        int maximumKi =
                ClientKiData.getMaximumKi();

        if (maximumKi <= 0) {
            return;
        }

        int currentKi =
                Mth.clamp(
                        ClientKiData.getCurrentKi(),
                        0,
                        maximumKi
                );

        boolean infiniteKi =
                minecraft.player.hasEffect(
                        MartialEffectRegistry
                                .STILLNESS_OF_MIND
                                .get()
                );

        /*
         * Same horizontal position as Iron's Hunger-anchored
         * mana bar: directly to the right of screen center.
         */
        int barX =
                screenWidth / 2
                        - BAR_WIDTH / 2
                        + 50;

        /*
         * Iron's mana overlay should already have increased
         * rightHeight because our overlay is registered above it.
         */
        int barY =
                screenHeight
                        - (getAndIncrementRightHeight(gui) - 2)
                        - BAR_HEIGHT / 2
                        - EXTRA_VERTICAL_SEPARATION;

        double fillRatio =
                infiniteKi
                        ? 1.0D
                        : Mth.clamp(
                        currentKi
                                / (double) maximumKi,
                        0.0D,
                        1.0D
                );

        int fillWidth =
                (int) Math.round(
                        BAR_WIDTH * fillRatio
                );

        RenderSystem.setShaderColor(
                TINT_RED,
                TINT_GREEN,
                TINT_BLUE,
                1.0F
        );

        graphics.blit(
                TEXTURE,
                barX,
                barY,
                SPRITE_X,
                EMPTY_SPRITE_Y,
                BAR_WIDTH,
                BAR_HEIGHT,
                TEXTURE_WIDTH,
                TEXTURE_HEIGHT
        );

        if (fillWidth > 0) {
            graphics.blit(
                    TEXTURE,
                    barX,
                    barY,
                    SPRITE_X,
                    FILLED_SPRITE_Y,
                    fillWidth,
                    BAR_HEIGHT,
                    TEXTURE_WIDTH,
                    TEXTURE_HEIGHT
            );
        }

        RenderSystem.setShaderColor(
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );

        String kiText =
                infiniteKi
                        ? "\u221E"
                        : currentKi
                        + "/"
                        + maximumKi;

        int textX =
                barX
                        + (
                        BAR_WIDTH
                                - gui.getFont().width(kiText)
                ) / 2;

        /*
         * Iron's normal 98-pixel mana bar places its text
         * lower than the XP-style bar.
         */
        int textY =
                barY + 11;

        graphics.drawString(
                gui.getFont(),
                kiText,
                textX,
                textY,
                TEXT_COLOR
        );
    }

    private static int getAndIncrementRightHeight(
            ForgeGui gui
    ) {
        int currentHeight =
                gui.rightHeight;

        /*
         * Reserve enough space for this bar so subsequent
         * right-side HUD elements do not overlap it.
         */
        gui.rightHeight +=
                BAR_HEIGHT + EXTRA_VERTICAL_SEPARATION;

        return currentHeight;
    }
}