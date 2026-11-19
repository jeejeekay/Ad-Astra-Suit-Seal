package IGoByJake.adastrasealing.client;

import IGoByJake.adastrasealing.AdAstraSealing;
import IGoByJake.adastrasealing.SealData;
import IGoByJake.adastrasealing.SealType;
import IGoByJake.adastrasealing.SealedTank;
import IGoByJake.adastrasealing.SuitLogic;
import com.mojang.blaze3d.vertex.PoseStack;
import earth.terrarium.adastra.api.systems.PlanetData;
import earth.terrarium.adastra.client.config.AdAstraConfigClient;
import earth.terrarium.adastra.client.utils.ClientData;
import earth.terrarium.adastra.common.items.armor.SpaceSuitItem;
import earth.terrarium.adastra.common.utils.TooltipUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = AdAstraSealing.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientEvents {
    // Ad Astra's own HUD art and tooltip text, so sealed armor looks exactly like a real suit.
    private static final ResourceLocation OXYGEN_TANK_EMPTY = new ResourceLocation("ad_astra", "textures/gui/sprites/overlay/oxygen_tank_empty.png");
    private static final ResourceLocation OXYGEN_TANK = new ResourceLocation("ad_astra", "textures/gui/sprites/overlay/oxygen_tank.png");
    private static final String AD_ASTRA_FLUID_LINE = "tooltip.ad_astra.fluid";

    private ClientEvents() {
    }

    /**
     * The oxygen bar. A copy of the oxygen part of Ad Astra's OverlayScreen.render (same textures, same position,
     * scale and colours, read from Ad Astra's client config), drawn only for suits that include a sealed piece.
     * Ad Astra draws its own bar for sets made only of its suit pieces, so the two never show together.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isSpectator()) return;
        if (minecraft.options.renderDebug) return;
        if (!SuitLogic.hasMixedSuit(player)) return;
        long capacity = SuitLogic.chestCapacity(player);
        if (capacity <= 0) return;
        long amount = SuitLogic.chestOxygen(player);

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        PoseStack poseStack = graphics.pose();

        double ratio = (double) amount / capacity;
        int barHeight = (int) (ratio * 52);

        int x = AdAstraConfigClient.oxygenBarX;
        int y = AdAstraConfigClient.oxygenBarY;
        float scale = AdAstraConfigClient.oxygenBarScale;

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        graphics.blit(OXYGEN_TANK_EMPTY, x, y, 0, 0, 62, 52, 62, 52);
        graphics.blit(OXYGEN_TANK, x, y + 52 - barHeight, 0, 52 - barHeight, 62, barHeight, 62, 52);

        String text = String.format("%.1f%%", ratio * 100);
        int textWidth = font.width(text);
        int color = ratio <= 0 ? 0xDC143C : 0xFFFFFF;
        PlanetData localData = ClientData.getLocalData();
        if (localData != null && localData.oxygen()) {
            color = 0x55ff55;
        }
        graphics.drawString(font, text, (int) (x + (62 - textWidth) / 2f), y + 52 + 3, color);
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        SealType seal = SealData.getSeal(stack);

        if (seal.isSealed()) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("tooltip.adastra_sealing.sealed." + seal.id())
                    .withStyle(seal.protectsFromHeat() ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE));
            if (SealData.hasTank(stack)) {
                lines.add(oxygenLine(SealData.getOxygen(stack), SealData.getCapacity(stack)));
            }
            // Ad Astra's own description of the suit this piece now works as ("Hold SHIFT for more information").
            TooltipUtils.addDescriptionComponent(lines, Component.translatable(
                    seal.protectsFromHeat() ? "info.ad_astra.netherite_space_suit" : "info.ad_astra.space_suit")
                    .withStyle(ChatFormatting.GRAY));
            // Straight under the item's name.
            tooltip.addAll(Math.min(1, tooltip.size()), lines);
            return;
        }

        // A real Ad Astra suit with Capacity Upgrades: its own oxygen line still shows the old size, so correct it.
        if (stack.getItem() instanceof SpaceSuitItem suit && SealData.getCapacityBonus(stack) > 0) {
            var tank = suit.getFluidContainer(stack);
            Component corrected = oxygenLine(tank.getFirstFluid().getFluidAmount(), tank.getTankCapacity(0));
            for (int i = 0; i < tooltip.size(); i++) {
                if (tooltip.get(i).getContents() instanceof TranslatableContents contents
                        && AD_ASTRA_FLUID_LINE.equals(contents.getKey())) {
                    tooltip.set(i, corrected);
                    break;
                }
            }
        }
    }

    /** "250 / 1K Oxygen" in gold: the same line, wording and number format Ad Astra shows on its suits. */
    private static Component oxygenLine(long amount, long capacity) {
        Component oxygenName = new FluidStack(SealedTank.oxygenFluid(), 1).getDisplayName();
        return Component.translatable(AD_ASTRA_FLUID_LINE,
                TooltipUtils.getFormattedAmount(amount),
                TooltipUtils.getFormattedAmount(capacity),
                oxygenName).withStyle(ChatFormatting.GOLD);
    }
}
