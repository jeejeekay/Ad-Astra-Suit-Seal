package IGoByJake.adastrasealing;

import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Decides when a mix of sealed pieces and real Ad Astra suit pieces counts as a suit.
 * <p>
 * A set made only of real Ad Astra pieces is always left to Ad Astra; this class only steps in once at least one
 * piece is sealed. The weakest piece decides: one piece without heat protection means no heat protection.
 */
public final class SuitLogic {
    public static final TagKey<Item> SPACE_SUITS = adAstraTag("space_suit_items");
    public static final TagKey<Item> NETHERITE_SPACE_SUITS = adAstraTag("netherite_space_suit_items");
    public static final TagKey<Item> FREEZE_RESISTANT = adAstraTag("freeze_resistant_armor");
    public static final TagKey<Item> HEAT_RESISTANT = adAstraTag("heat_resistant_armor");
    /** Items a pack maker never wants sealed. Empty by default. */
    public static final TagKey<Item> CANNOT_BE_SEALED =
            TagKey.create(Registries.ITEM, new ResourceLocation(AdAstraSealing.MODID, "cannot_be_sealed"));

    private SuitLogic() {
    }

    private static TagKey<Item> adAstraTag(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation("ad_astra", name));
    }

    /** The armor slot an item is worn in, or null if it is not worn as armor. */
    @Nullable
    public static EquipmentSlot armorSlot(ItemStack stack) {
        if (stack.isEmpty()) return null;
        EquipmentSlot slot = LivingEntity.getEquipmentSlotForItem(stack);
        return slot.getType() == EquipmentSlot.Type.ARMOR ? slot : null;
    }

    /** Chest items get the oxygen tank. Checked on the item alone so it is safe while a stack is still being built. */
    public static boolean isChestItem(Item item) {
        return item instanceof Equipable equipable && equipable.getEquipmentSlot() == EquipmentSlot.CHEST;
    }

    /** Real Ad Astra suit pieces are never sealed: they already are a suit. */
    public static boolean isRealSuitPiece(ItemStack stack) {
        return stack.getItem() instanceof SpaceSuitItem || stack.is(SPACE_SUITS);
    }

    private static boolean allArmor(LivingEntity entity, Predicate<ItemStack> test) {
        for (ItemStack stack : entity.getArmorSlots()) {
            if (!test.test(stack)) return false;
        }
        return true;
    }

    public static boolean wearsAnySealed(LivingEntity entity) {
        for (ItemStack stack : entity.getArmorSlots()) {
            if (SealData.isSealed(stack)) return true;
        }
        return false;
    }

    /** At least one sealed piece, and every armor slot holds a sealed piece or a real suit piece. */
    public static boolean hasMixedSuit(LivingEntity entity) {
        return wearsAnySealed(entity) && allArmor(entity, s -> SealData.isSealed(s) || s.is(SPACE_SUITS));
    }

    /** Oxygen the worn chest piece holds right now, in millibuckets: a sealed tank or a real suit's tank. */
    public static long chestOxygen(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (SealData.hasTank(chest)) return SealData.getOxygen(chest);
        if (chest.getItem() instanceof SpaceSuitItem suit) {
            return suit.getFluidContainer(chest).getFirstFluid().getFluidAmount();
        }
        return 0;
    }

    /** Capacity of the worn chest piece's tank, in millibuckets. */
    public static long chestCapacity(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (SealData.hasTank(chest)) return SealData.getCapacity(chest);
        if (chest.getItem() instanceof SpaceSuitItem suit) {
            return suit.getFluidContainer(chest).getTankCapacity(0);
        }
        return 0;
    }

    /** True when a mixed suit is keeping the wearer breathing, so Ad Astra's suffocation must not run. */
    public static boolean suppliesOxygen(LivingEntity entity) {
        if (!hasMixedSuit(entity)) return false;
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (SealData.hasTank(chest)) return SealData.getOxygen(chest) > 0;
        // A real suit on the chest with sealed pieces around it: same "more than 1 mB" rule Ad Astra uses.
        return chestOxygen(entity) > 1;
    }

    public static boolean protectsFromCold(LivingEntity entity) {
        return wearsAnySealed(entity) && allArmor(entity, s -> SealData.isSealed(s) || s.is(FREEZE_RESISTANT));
    }

    public static boolean protectsFromHeat(LivingEntity entity) {
        return wearsAnySealed(entity)
                && allArmor(entity, s -> SealData.getSeal(s).protectsFromHeat() || s.is(HEAT_RESISTANT));
    }

    /** Ad Astra makes a full netherite space suit immune to fire; insulated sealing matches it. */
    public static boolean isFireImmune(LivingEntity entity) {
        return wearsAnySealed(entity)
                && allArmor(entity, s -> SealData.getSeal(s).protectsFromHeat() || s.is(NETHERITE_SPACE_SUITS));
    }
}
