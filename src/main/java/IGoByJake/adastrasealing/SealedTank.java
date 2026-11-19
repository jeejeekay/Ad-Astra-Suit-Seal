package IGoByJake.adastrasealing;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The oxygen tank of a sealed chestplate, exposed as Forge's item fluid handler.
 * <p>
 * Botarium (Ad Astra's library) finds item tanks through this same Forge capability, so the oxygen loader and held
 * gas tanks fill a sealed chestplate exactly the way they fill a real suit. The tank only exists while the item is
 * sealed, and it reads and writes the item's NBT directly, so copies of the stack never go stale.
 */
public class SealedTank implements ICapabilityProvider, IFluidHandlerItem {
    public static final ResourceLocation ID = new ResourceLocation(AdAstraSealing.MODID, "oxygen_tank");
    public static final ResourceLocation OXYGEN_ID = new ResourceLocation("ad_astra", "oxygen");
    public static final TagKey<Fluid> OXYGEN_TAG = TagKey.create(Registries.FLUID, OXYGEN_ID);

    private final ItemStack stack;
    private final LazyOptional<IFluidHandlerItem> handler = LazyOptional.of(() -> this);

    public SealedTank(ItemStack stack) {
        this.stack = stack;
    }

    /** Ad Astra's oxygen fluid, or empty if it is somehow missing. */
    public static Fluid oxygenFluid() {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(OXYGEN_ID);
        return fluid == null ? Fluids.EMPTY : fluid;
    }

    @SuppressWarnings("deprecation")
    public static boolean isOxygen(FluidStack fluid) {
        return !fluid.isEmpty() && fluid.getFluid().is(OXYGEN_TAG);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER_ITEM && SealData.hasTank(stack)) {
            return handler.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public @NotNull ItemStack getContainer() {
        return stack;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        long amount = SealData.getOxygen(stack);
        Fluid oxygen = oxygenFluid();
        if (amount <= 0 || oxygen == Fluids.EMPTY) return FluidStack.EMPTY;
        return new FluidStack(oxygen, (int) amount);
    }

    @Override
    public int getTankCapacity(int tank) {
        return (int) SealData.getCapacity(stack);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack fluid) {
        return isOxygen(fluid);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!isOxygen(resource)) return 0;
        long stored = SealData.getOxygen(stack);
        long filled = Math.max(0, Math.min(resource.getAmount(), SealData.getCapacity(stack) - stored));
        if (filled > 0 && action.execute()) {
            SealData.setOxygen(stack, stored + filled);
        }
        return (int) filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (!isOxygen(resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        long stored = SealData.getOxygen(stack);
        long drained = Math.max(0, Math.min(maxDrain, stored));
        Fluid oxygen = oxygenFluid();
        if (drained <= 0 || oxygen == Fluids.EMPTY) return FluidStack.EMPTY;
        if (action.execute()) {
            SealData.setOxygen(stack, stored - drained);
        }
        return new FluidStack(oxygen, (int) drained);
    }
}
