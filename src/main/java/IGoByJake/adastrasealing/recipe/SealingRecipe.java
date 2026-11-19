package IGoByJake.adastrasealing.recipe;

import IGoByJake.adastrasealing.AdAstraSealing;
import IGoByJake.adastrasealing.SealData;
import IGoByJake.adastrasealing.SealType;
import IGoByJake.adastrasealing.SealedTank;
import IGoByJake.adastrasealing.SuitLogic;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;

/**
 * Smithing table: sealing template + any armor piece + the matching suit piece = the same armor piece, sealed.
 * <p>
 * The result is a copy of the armor piece with a marker added, so its looks, stats, enchantments, damage and any
 * other NBT stay as they were. Vanilla's smithing_transform can't do this for "any item", so this is its own type.
 *
 * <pre>
 * {
 *   "type": "adastra_sealing:sealing",
 *   "template": { "item": "adastra_sealing:space_suit_sealing_template" },
 *   "addition": { "tag": "adastra_sealing:space_suit_pieces" },
 *   "seal": "space"
 * }
 * </pre>
 * "seal" is "space" or "insulated". The addition must be worn in the same slot as the armor piece.
 */
public class SealingRecipe implements SmithingRecipe {
    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient addition;
    private final SealType seal;

    public SealingRecipe(ResourceLocation id, Ingredient template, Ingredient addition, SealType seal) {
        this.id = id;
        this.template = template;
        this.addition = addition;
        this.seal = seal;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    /** Any armor that isn't already sealed this well. A space-sealed piece can still be made insulated. */
    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        EquipmentSlot slot = SuitLogic.armorSlot(stack);
        if (slot == null) return false;
        if (SuitLogic.isRealSuitPiece(stack) || stack.is(SuitLogic.CANNOT_BE_SEALED)) return false;
        // The oxygen tank can only be attached to ordinary chest items.
        if (slot == EquipmentSlot.CHEST && !SuitLogic.isChestItem(stack.getItem())) return false;
        return SealData.getSeal(stack).ordinal() < seal.ordinal();
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean matches(Container container, Level level) {
        ItemStack base = container.getItem(1);
        ItemStack suitPiece = container.getItem(2);
        if (!template.test(container.getItem(0)) || !isBaseIngredient(base) || !addition.test(suitPiece)) return false;
        // A helmet is sealed with a suit helmet, boots with suit boots, and so on.
        return SuitLogic.armorSlot(base) == SuitLogic.armorSlot(suitPiece);
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        ItemStack result = container.getItem(1).copyWithCount(1);
        SealData.setSeal(result, seal);
        if (SealData.hasTank(result)) {
            // Oxygen left in the suit chestplate being used up moves into the new tank.
            long carried = oxygenIn(container.getItem(2));
            if (carried > 0) {
                SealData.setOxygen(result, SealData.getOxygen(result) + carried);
            }
        }
        return result;
    }

    private static long oxygenIn(ItemStack stack) {
        if (stack.getItem() instanceof SpaceSuitItem suit) {
            return suit.getFluidContainer(stack).getFirstFluid().getFluidAmount();
        }
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(tank -> {
            FluidStack fluid = tank.getFluidInTank(0);
            return SealedTank.isOxygen(fluid) ? (long) fluid.getAmount() : 0L;
        }).orElse(0L);
    }

    /** The result depends on the armor piece put in, so there is no fixed item to show. */
    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean isIncomplete() {
        return template.isEmpty() || addition.isEmpty();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return AdAstraSealing.SEALING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<SealingRecipe> {
        @Override
        public SealingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient template = Ingredient.fromJson(json.get("template"));
            Ingredient addition = Ingredient.fromJson(json.get("addition"));
            String sealName = GsonHelper.getAsString(json, "seal");
            SealType seal = SealType.byId(sealName);
            if (!seal.isSealed()) {
                throw new JsonSyntaxException("Unknown seal '" + sealName + "', expected 'space' or 'insulated'");
            }
            return new SealingRecipe(id, template, addition, seal);
        }

        @Override
        public SealingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient template = Ingredient.fromNetwork(buffer);
            Ingredient addition = Ingredient.fromNetwork(buffer);
            SealType seal = buffer.readEnum(SealType.class);
            return new SealingRecipe(id, template, addition, seal);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SealingRecipe recipe) {
            recipe.template.toNetwork(buffer);
            recipe.addition.toNetwork(buffer);
            buffer.writeEnum(recipe.seal);
        }
    }
}
