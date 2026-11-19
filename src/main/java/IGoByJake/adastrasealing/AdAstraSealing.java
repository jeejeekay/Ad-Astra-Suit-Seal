package IGoByJake.adastrasealing;

import IGoByJake.adastrasealing.recipe.CapacityUpgradeRecipe;
import IGoByJake.adastrasealing.recipe.SealingRecipe;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

@Mod(AdAstraSealing.MODID)
public class AdAstraSealing {
    public static final String MODID = "adastra_sealing";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

    // Vanilla's own empty-slot outlines, shown in the smithing table like they are for armor trims.
    private static final List<ResourceLocation> ARMOR_ICONS = List.of(
            new ResourceLocation("item/empty_armor_slot_helmet"),
            new ResourceLocation("item/empty_armor_slot_chestplate"),
            new ResourceLocation("item/empty_armor_slot_leggings"),
            new ResourceLocation("item/empty_armor_slot_boots"));
    private static final List<ResourceLocation> CHEST_ICON = List.of(
            new ResourceLocation("item/empty_armor_slot_chestplate"));
    private static final List<ResourceLocation> TANK_ICON = List.of(
            new ResourceLocation(MODID, "item/empty_slot_tank"));

    public static final RegistryObject<Item> SPACE_SUIT_SEALING_TEMPLATE =
            ITEMS.register("space_suit_sealing_template", () -> template("space_suit_sealing", ARMOR_ICONS, ARMOR_ICONS));
    public static final RegistryObject<Item> INSULATED_SUIT_SEALING_TEMPLATE =
            ITEMS.register("insulated_suit_sealing_template", () -> template("insulated_suit_sealing", ARMOR_ICONS, ARMOR_ICONS));
    public static final RegistryObject<Item> CAPACITY_UPGRADE_TEMPLATE =
            ITEMS.register("capacity_upgrade_template", () -> template("capacity_upgrade", CHEST_ICON, TANK_ICON));

    public static final RegistryObject<RecipeSerializer<SealingRecipe>> SEALING_SERIALIZER =
            RECIPE_SERIALIZERS.register("sealing", SealingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<CapacityUpgradeRecipe>> CAPACITY_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("capacity_upgrade", CapacityUpgradeRecipe.Serializer::new);

    public AdAstraSealing() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addToCreativeTabs);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SealingConfig.SPEC);
    }

    /**
     * A smithing template in the vanilla style: the item is called "Smithing Template" with the upgrade's name
     * under it, plus the "Applies to" and "Ingredients" lines. The text is in the lang file under
     * {@code upgrade.adastra_sealing.<name>} and {@code item.adastra_sealing.smithing_template.<name>.*}.
     */
    private static SmithingTemplateItem template(String name, List<ResourceLocation> baseIcons, List<ResourceLocation> additionIcons) {
        String key = "item." + MODID + ".smithing_template." + name;
        return new SmithingTemplateItem(
                Component.translatable(key + ".applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable(key + ".ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("upgrade." + MODID + "." + name).withStyle(ChatFormatting.GRAY),
                Component.translatable(key + ".base_slot_description"),
                Component.translatable(key + ".additions_slot_description"),
                baseIcons,
                additionIcons);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Ad Astra keeps its listeners in plain lists, so add ours on the main thread.
        event.enqueueWork(AdAstraHooks::register);
    }

    private void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        boolean adAstraTab = "ad_astra".equals(event.getTabKey().location().getNamespace());
        if (adAstraTab || event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(SPACE_SUIT_SEALING_TEMPLATE);
            event.accept(INSULATED_SUIT_SEALING_TEMPLATE);
            event.accept(CAPACITY_UPGRADE_TEMPLATE);
        }
    }
}
