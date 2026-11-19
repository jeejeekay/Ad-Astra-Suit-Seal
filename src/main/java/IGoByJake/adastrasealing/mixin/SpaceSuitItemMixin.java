package IGoByJake.adastrasealing.mixin;

import IGoByJake.adastrasealing.SealData;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import earth.terrarium.botarium.common.fluid.base.FluidHolder;
import earth.terrarium.botarium.common.fluid.impl.SimpleFluidContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiPredicate;

/**
 * Lets a Capacity Upgrade make a real Ad Astra suit's oxygen tank bigger.
 * <p>
 * Ad Astra builds a suit's tank with a fixed size. This swaps that fixed size for "the suit's size plus this
 * chestplate's CapacityBonus", read from the item each time it is asked, so the suit's own bar, HUD, loader and
 * gas tank refilling all see one bigger tank.
 * <p>
 * The target is Ad Astra's own method (not obfuscated), hence remap = false and no refmap. If a different Ad Astra
 * version doesn't match, the mixin is skipped quietly and CapacityUpgradeRecipe refuses to upgrade real suits.
 */
@Mixin(value = SpaceSuitItem.class, remap = false)
public abstract class SpaceSuitItemMixin {

    @Redirect(
            method = "getFluidContainer(Lnet/minecraft/world/item/ItemStack;)Learth/terrarium/botarium/common/fluid/impl/WrappedItemFluidContainer;",
            at = @At(
                    value = "NEW",
                    target = "(JILjava/util/function/BiPredicate;)Learth/terrarium/botarium/common/fluid/impl/SimpleFluidContainer;"),
            require = 0)
    private SimpleFluidContainer adastra_sealing$upgradableTank(long maxAmount, int tanks, BiPredicate<Integer, FluidHolder> filter, ItemStack holder) {
        return new SimpleFluidContainer(tank -> maxAmount + SealData.getCapacityBonus(holder), tanks, filter);
    }
}
