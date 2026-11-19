package IGoByJake.adastrasealing;

import earth.terrarium.adastra.api.systems.OxygenApi;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AdAstraSealing.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonEvents {
    private CommonEvents() {
    }

    /** Every chest item gets the (dormant) oxygen tank; it only switches on once the item is sealed. */
    @SubscribeEvent
    public static void onAttachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        Item item = event.getObject().getItem();
        if (!SuitLogic.isChestItem(item) || item instanceof SpaceSuitItem) return;
        event.addCapability(SealedTank.ID, new SealedTank(event.getObject()));
    }

    /**
     * A worn sealed chestplate behaves like Ad Astra's space suit chestplate (SpaceSuitItem.inventoryTick):
     * it stops powder-snow style freezing, uses 1 mB of oxygen every 12 ticks where there is no oxygen
     * (10 minutes per 1,000 mB), and lets the wearer breathe underwater for 1 mB more.
     */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!SealData.hasTank(chest)) return;
        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return;

        entity.setTicksFrozen(0);
        if (entity.tickCount % 12 != 0) return;
        long oxygen = SealData.getOxygen(chest);
        if (oxygen <= 0) return;

        long used = 0;
        if (!OxygenApi.API.hasOxygen(entity)) {
            used++;
        }
        if (entity.isEyeInFluid(FluidTags.WATER)) {
            used++;
            entity.setAirSupply(Math.min(entity.getMaxAirSupply(), entity.getAirSupply() + 4 * 10));
        }
        if (used > 0) {
            SealData.setOxygen(chest, oxygen - used);
        }
    }

    /** Fire and lava immunity for a full insulated set, as Ad Astra gives a full netherite space suit. */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (!source.is(DamageTypeTags.IS_FIRE) && !source.is(DamageTypes.HOT_FLOOR)) return;
        if (!SealingConfig.insulatedFireImmunity()) return;
        LivingEntity entity = event.getEntity();
        if (!SuitLogic.isFireImmune(entity)) return;
        entity.setRemainingFireTicks(0);
        event.setCanceled(true);
    }
}
