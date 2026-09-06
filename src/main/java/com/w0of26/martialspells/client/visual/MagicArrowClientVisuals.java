package com.w0of26.martialspells.client.visual;

import com.w0of26.martialspells.ranged.RangedWeaponClassifier;
import com.w0of26.martialspells.spells.BarrageSpell;
import io.redspace.ironsspellbooks.capabilities.magic.SyncedSpellData;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class MagicArrowClientVisuals {
    public static final String MAGIC_ARROW_ID = "irons_spellbooks:magic_arrow";
    public static final String BARRAGE_ID = "martial_spells:barrage";
    public static final String ENTANGLING_ARROW_ID = "martial_spells:entangling_arrow";
    private static final float MAGIC_ARROW_CROSSBOW_CHARGE_END_PROGRESS = 2.0F / 3.0F;
    private static final float BARRAGE_PREPARE_END_PROGRESS =
            (float) BarrageSpell.PREPARE_TICKS / (float) BarrageSpell.CAST_TIME_TICKS;

    private MagicArrowClientVisuals() {}

    public static boolean isMagicArrowCasting(@Nullable LivingEntity entity) { return isCastingSpell(entity, MAGIC_ARROW_ID); }
    public static boolean isBarrageCasting(@Nullable LivingEntity entity) { return isCastingSpell(entity, BARRAGE_ID); }
    public static boolean isEntanglingArrowCasting(@Nullable LivingEntity entity) {
        return isCastingSpell(entity, ENTANGLING_ARROW_ID);
    }
    public static boolean isRangedSpellCasting(@Nullable LivingEntity entity) {
        return isMagicArrowCasting(entity) || isBarrageCasting(entity) || isEntanglingArrowCasting(entity);
    }

    private static boolean isCastingSpell(@Nullable LivingEntity entity, String spellId) {
        return entity instanceof Player player && spellId.equals(getCastingSpellId(player));
    }

    private static String getCastingSpellId(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == player) {
            return ClientMagicData.isCasting() ? ClientMagicData.getCastingSpellId() : "";
        }
        SyncedSpellData synced = ClientMagicData.getSyncedSpellData(player);
        return synced.isCasting() ? synced.getCastingSpellId() : "";
    }

    @Nullable
    public static InteractionHand getSelectedRangedHand(Player player) {
        if (RangedWeaponClassifier.isSupported(player.getMainHandItem())) return InteractionHand.MAIN_HAND;
        if (RangedWeaponClassifier.isSupported(player.getOffhandItem())) return InteractionHand.OFF_HAND;
        return null;
    }

    public static RangedWeaponClassifier.Type getSelectedRangedType(Player player) {
        InteractionHand hand = getSelectedRangedHand(player);
        return hand == null ? RangedWeaponClassifier.Type.NONE
                : RangedWeaponClassifier.classify(player.getItemInHand(hand));
    }

    @Nullable
    public static HumanoidArm getSelectedRangedArm(Player player) {
        InteractionHand hand = getSelectedRangedHand(player);
        if (hand == null) return null;
        return hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    public static boolean isSelectedRangedStack(@Nullable LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof Player player)) return false;
        InteractionHand hand = getSelectedRangedHand(player);
        return hand != null && player.getItemInHand(hand) == stack;
    }

    @Nullable
    public static Player findSelectedRangedStackHolder(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return null;
        for (Player player : minecraft.level.players()) {
            if (!isRangedSpellCasting(player)) continue;
            InteractionHand hand = getSelectedRangedHand(player);
            if (hand != null && player.getItemInHand(hand) == stack) return player;
        }
        return null;
    }

    public static float getCastProgress(@Nullable LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (entity != null && minecraft.player == entity && isRangedSpellCasting(entity)) {
            return Mth.clamp(ClientMagicData.getCastCompletionPercent(), 0.0F, 1.0F);
        }
        return 1.0F;
    }

    public static float getWeaponPreparationProgress(@Nullable LivingEntity entity) {
        float raw = getCastProgress(entity);
        return isBarrageCasting(entity)
                ? Mth.clamp(raw / BARRAGE_PREPARE_END_PROGRESS, 0.0F, 1.0F)
                : raw;
    }

    public static float getCrossbowChargeProgress(@Nullable LivingEntity entity) {
        if (isBarrageCasting(entity)) return getWeaponPreparationProgress(entity);
        return Mth.clamp(getCastProgress(entity) / MAGIC_ARROW_CROSSBOW_CHARGE_END_PROGRESS, 0.0F, 1.0F);
    }

    public static boolean isCrossbowReady(@Nullable LivingEntity entity) {
        return isBarrageCasting(entity)
                ? getWeaponPreparationProgress(entity) >= 1.0F
                : getCastProgress(entity) >= MAGIC_ARROW_CROSSBOW_CHARGE_END_PROGRESS;
    }
}
