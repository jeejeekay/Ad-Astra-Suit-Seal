package IGoByJake.adastrasealing.recipe;

import IGoByJake.adastrasealing.AdAstraSealing;
import IGoByJake.adastrasealing.SealData;
import IGoByJake.adastrasealing.SealingConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;

/**
 * Smithing table: Capacity Upgrade template + a chestplate with an oxygen tank + a tank = a bigger oxygen tank.
 * <p>
 * Works on sealed chestplates and on Ad Astra's own suit chestplates. It can be repeated until the tank reaches
 * the config's maxCapacity; the last upgrade is cut short if it would go over.
 *
 * <pre>
 * {
 *   "type": "adastra_sealing:capacity_upgrade",
 *   "template": { "item": "adastra_sealing:capacity_upgrade_template" },
 *   "addition": { "item": "ad_astra:desh_tank" },
 *   "amount": 250
 * }
 * </pre>
 * "amount" is the capacity added, in millibuckets.
 */
public class CapacityUpgradeRecipe implements SmithingRecipe {
    private final ResourceLocation id;
    private final Ingredient template;
    private final Ingredient addition;
    private final int amount;

    public CapacityUpgradeRecipe(ResourceLocation id, Ingredient template, Ingredient addition, int amount) {
        this.id = id;
        this.template = template;
        this.addition = addition;
        this.amount = amount;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return SealData.hasTank(stack) || stack.getItem() instanceof SpaceSuitItem;
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return template.test(container.getItem(0))
                && addition.test(container.getItem(2))
                && !upgrade(container.getItem(1)).isEmpty();
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return upgrade(container.getItem(1));
    }

    /** The upgraded copy, or empty if this chestplate can't take the upgrade. */
    private ItemStack upgrade(ItemStack base) {
        int max = SealingConfig.maxCapacity();
        if (SealData.hasTank(base)) {
            long room = max - SealData.getCapacity(base);
            if (room <= 0) return ItemStack.EMPTY;
            ItemStack result = base.copyWithCount(1);
            SealData.setCapacityBonus(result, SealData.getCapacityBonus(base) + Math.min(amount, room));
            return result;
        }
        if (base.getItem() instanceof SpaceSuitItem suit) {
            // The suit's real tank grows (see SpaceSuitItemMixin), so Ad Astra's own display and refilling keep working.
            long before = suit.getFluidContainer(base).getTankCapacity(0);
            long added = Math.min(amount, max - before);
            if (added <= 0) return ItemStack.EMPTY;
            ItemStack result = base.copyWithCount(1);
            SealData.setCapacityBonus(result, SealData.getCapacityBonus(base) + added);
            // If the mixin didn't apply (a different Ad Astra version), the tank would not grow: refuse the
            // upgrade instead of eating the player's items.
            long after = suit.getFluidContainer(result).getTankCapacity(0);
            return after == before + added ? result : ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

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
        return AdAstraSealing.CAPACITY_UPGRADE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<CapacityUpgradeRecipe> {
        @Override
        public CapacityUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient template = Ingredient.fromJson(json.get("template"));
            Ingredient addition = Ingredient.fromJson(json.get("addition"));
            int amount = GsonHelper.getAsInt(json, "amount");
            if (amount <= 0) {
                throw new JsonSyntaxException("'amount' must be more than 0");
            }
            return new CapacityUpgradeRecipe(id, template, addition, amount);
        }

        @Override
        public CapacityUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient template = Ingredient.fromNetwork(buffer);
            Ingredient addition = Ingredient.fromNetwork(buffer);
            int amount = buffer.readVarInt();
            return new CapacityUpgradeRecipe(id, template, addition, amount);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CapacityUpgradeRecipe recipe) {
            recipe.template.toNetwork(buffer);
            recipe.addition.toNetwork(buffer);
            buffer.writeVarInt(recipe.amount);
        }
    }
}
