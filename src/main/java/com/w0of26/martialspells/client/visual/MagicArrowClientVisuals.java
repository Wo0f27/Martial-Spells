package com.w0of26.martialspells.client.visual;

import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only render state shared by ranged channel spells that need a
 * real held bow/crossbow to visually participate in the cast.
 *
 * Nothing here mutates item-use state or crossbow NBT; it only answers
 * questions consumed by render hooks.
 */
public final class MagicArrowClientVisuals {
    public static final String MAGIC_ARROW_ID =
            "irons_spellbooks:magic_arrow";
    public static final String BARRAGE_ID =
            "martial_spells:barrage";

    private MagicArrowClientVisuals() {
    }

    public static boolean isMagicArrowCasting(
            @Nullable LivingEntity entity
    ) {
        return isCastingSpell(entity, MAGIC_ARROW_ID);
    }

    public static boolean isRangedChannelCasting(
            @Nullable LivingEntity entity
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        String castingSpellId = getCastingSpellId(player);
        return MAGIC_ARROW_ID.equals(castingSpellId)
                || BARRAGE_ID.equals(castingSpellId);
    }

    private static boolean isCastingSpell(
            @Nullable LivingEntity entity,
            String spellId
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        return spellId.equals(getCastingSpellId(player));
    }

    private static String getCastingSpellId(Player player) {
        Minecraft minecraft = Minecraft.getInstance();

        /*
         * The local player's exact cast state lives in MagicData.
         * Remote players are represented by Iron's synchronized spell
         * data, so third-person multiplayer rendering works as well.
         */
        if (minecraft.player == player) {
            return ClientMagicData.isCasting()
                    ? ClientMagicData.getCastingSpellId()
                    : "";
        }

        SyncedSpellData synced =
                ClientMagicData.getSyncedSpellData(player);

        return synced.isCasting()
                ? synced.getCastingSpellId()
                : "";
    }

    public static boolean isHeldRangedStack(
            @Nullable LivingEntity entity,
            ItemStack stack
    ) {
        if (!(entity instanceof Player player)) {
            return false;
        }

        return stack == player.getMainHandItem()
                || stack == player.getOffhandItem();
    }

    /**
     * Exact local visual pull progress. Remote players do not receive
     * the effective cast duration, so they render as fully drawn while
     * the synchronized cast flag is active.
     */
    public static float getBowPullProgress(
            @Nullable LivingEntity entity
    ) {
        Minecraft minecraft = Minecraft.getInstance();

        if (entity != null && minecraft.player == entity) {
            return Mth.clamp(
                    ClientMagicData.getCastCompletionPercent(),
                    0.0F,
                    1.0F
            );
        }

        return 1.0F;
    }

    /**
     * Returns the physical player arm currently holding the crossbow,
     * preferring the main hand if both hands contain crossbows.
     */
    @Nullable
    public static HumanoidArm getCrossbowArm(Player player) {
        if (player.getMainHandItem().getItem()
                instanceof CrossbowItem) {
            return player.getMainArm();
        }

        if (player.getOffhandItem().getItem()
                instanceof CrossbowItem) {
            return player.getMainArm().getOpposite();
        }

        return null;
    }

    public static boolean isSupportedRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }
}
