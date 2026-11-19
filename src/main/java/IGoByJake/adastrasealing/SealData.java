package IGoByJake.adastrasealing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Everything this mod stores on an item lives in one NBT compound, {@value #ROOT}:
 * <ul>
 *   <li>{@code Seal}: "space" or "insulated" (sealed armor only)</li>
 *   <li>{@code Oxygen}: millibuckets of oxygen in a sealed chestplate's own tank</li>
 *   <li>{@code CapacityBonus}: extra tank capacity from Capacity Upgrades (sealed chestplates and Ad Astra suits)</li>
 * </ul>
 * It is ordinary item NBT, so it survives anvils, enchanting, repairs and vanilla smithing upgrades.
 */
public final class SealData {
    public static final String ROOT = "AdAstraSealing";
    private static final String SEAL = "Seal";
    private static final String OXYGEN = "Oxygen";
    private static final String BONUS = "CapacityBonus";

    private SealData() {
    }

    @Nullable
    private static CompoundTag read(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND)) return null;
        return tag.getCompound(ROOT);
    }

    public static SealType getSeal(ItemStack stack) {
        CompoundTag data = read(stack);
        return data == null ? SealType.NONE : SealType.byId(data.getString(SEAL));
    }

    public static boolean isSealed(ItemStack stack) {
        return getSeal(stack).isSealed();
    }

    public static void setSeal(ItemStack stack, SealType type) {
        stack.getOrCreateTagElement(ROOT).putString(SEAL, type.id());
    }

    /** True for a sealed item that is worn on the chest: those carry their own oxygen tank. */
    public static boolean hasTank(ItemStack stack) {
        return !stack.isEmpty() && SuitLogic.isChestItem(stack.getItem()) && isSealed(stack);
    }

    public static long getCapacityBonus(ItemStack stack) {
        CompoundTag data = read(stack);
        return data == null ? 0 : Math.max(0, data.getLong(BONUS));
    }

    public static void setCapacityBonus(ItemStack stack, long bonus) {
        stack.getOrCreateTagElement(ROOT).putLong(BONUS, Math.max(0, bonus));
    }

    /** Capacity of a sealed chestplate's own tank, in millibuckets. */
    public static long getCapacity(ItemStack stack) {
        long capacity = SealingConfig.baseCapacity() + getCapacityBonus(stack);
        return Math.max(1, Math.min(capacity, Math.max(SealingConfig.baseCapacity(), SealingConfig.maxCapacity())));
    }

    /** Oxygen in a sealed chestplate's own tank, in millibuckets. */
    public static long getOxygen(ItemStack stack) {
        CompoundTag data = read(stack);
        return data == null ? 0 : Math.max(0, Math.min(data.getLong(OXYGEN), getCapacity(stack)));
    }

    public static void setOxygen(ItemStack stack, long amount) {
        stack.getOrCreateTagElement(ROOT).putLong(OXYGEN, Math.max(0, Math.min(amount, getCapacity(stack))));
    }
}
