package com.daratrix.ronapi.cursor;

import com.daratrix.ronapi.ai.registers.AiGameRuleRegister;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.registers.GameRuleRegister;
import com.daratrix.ronapi.utils.RenderingUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.building.BuildingUtils;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;


import java.util.List;

public class CursorClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final int iconSize = 14;

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render evt) {
        long window = MC.getWindow().getWindow();

        if (!OrthoviewClientEvents.isEnabled()
                || !(evt.getScreen() instanceof TopdownGui)) {
            if (GLFW.glfwRawMouseMotionSupported()) // raw mouse increases sensitivity massively for some reason
                GLFW.glfwSetInputMode(window, GLFW.GLFW_RAW_MOUSE_MOTION, GLFW.GLFW_TRUE);
            GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            return;
        }

        if (MC.player == null || MC.level == null)
            return;

        if (!GameRuleRegister.showDebug(MC.getSingleplayerServer())) {
            return;
        }

        GuiGraphics guiGraphics = evt.getGuiGraphics();
        var highlightedPos = com.solegendary.reignofnether.cursor.CursorClientEvents.getPreselectedBlockPos();
        var mouseX = evt.getMouseX();
        var mouseY = evt.getMouseY();
        var iconX = mouseX + iconSize;
        var iconY = mouseY < MC.screen.height / 2 ? mouseY - iconSize + 10 : mouseY - iconSize;
        var resourceNode = WorldApi.getResourceAt(highlightedPos);

        if (resourceNode != null) {
            //AABB box = new AABB(resourceNode.getMinPos(), resourceNode.getMaxPos());
            //MyRenderer.drawLineBox(guiGraphics, box, 1, 1, 0, 1);
            var lines = List.of(
                    //FormattedCharSequence.forward(resourceNode.getResourceType().name() + " node", Style.EMPTY),
                    FormattedCharSequence.forward("      " + resourceNode.getAmount(), Style.EMPTY));
            MyRenderer.renderTooltip(guiGraphics, lines, mouseX, mouseY);
            MyRenderer.renderIcon(guiGraphics, getResourceIcon(resourceNode.getResourceType()), iconX, iconY, iconSize);
        } else {
            var resourceBlock = ResourceSources.getFromBlockPos(highlightedPos, MC.level);
            if (resourceBlock != null && resourceBlock.resourceValue > 1 && !BuildingUtils.isPosInsideAnyBuilding(true, highlightedPos)) {
                //MyRenderer.drawBlockOutline(guiGraphics, highlightedPos, 1);
                var lines = List.of(
                        //FormattedCharSequence.forward(resourceBlock.resourceName.name() + " block", Style.EMPTY),
                        FormattedCharSequence.forward("      " + resourceBlock.resourceValue, Style.EMPTY));
                MyRenderer.renderTooltip(guiGraphics, lines, mouseX, mouseY);
                MyRenderer.renderIcon(guiGraphics, getResourceIcon(resourceBlock.resourceName), iconX, iconY, iconSize);
            }
        }

        /*if (HudClientEvents.hudSelectedEntity != null) {
            var unit = WorldApi.getSingleton().units.getOrDefault(HudClientEvents.hudSelectedEntity, null);
            if (unit != null) {
                var orderId = unit.getCurrentOrderId();
                var lines = List.of(
                        FormattedCharSequence.forward(unit.getX() + "x, " + unit.getZ() + "y", Style.EMPTY),
                        FormattedCharSequence.forward(TypeIds.toItemName(orderId) + "(" + orderId + ")", Style.EMPTY));
                MyRenderer.renderTooltip(guiGraphics, lines, mouseX, mouseY);
            }
        }*/
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {

        if (!GameRuleRegister.showDebug(MC.getSingleplayerServer())) {
            return;
        }

        PoseStack poseStack = evt.getPoseStack();
        var highlightedPos = com.solegendary.reignofnether.cursor.CursorClientEvents.getPreselectedBlockPos();
        var highligthedResource = WorldApi.getResourceAt(highlightedPos);

        if (highligthedResource != null) {
            //AABB box = new AABB(highligthedResource.getMinPos(), highligthedResource.getMaxPos());
            //MyRenderer.drawLineBoxOutlineOnly(poseStack, RenderingUtils.TranslucentVertexConsumer, highligthedResource.getBoundingBox(), 1, 1, 0, 0.5f, false);
            //MyRenderer.drawLineBoxOutlineOnly(poseStack, highligthedResource.getBoundingBox(), 1, 1, 0, 0.5f, false);
            //for(var block: highligthedResource.getBlocks().toList()) {
            //    MyRenderer.drawBlockOutline(poseStack, block, 1);
            //}
            //MyRenderer.drawBlockOutline(poseStack, highligthedResource.getMinPos(), 1);
            //MyRenderer.drawBlockOutline(poseStack, highligthedResource.getMaxPos(), 1);
        } else {
            var resourceBlock = ResourceSources.getFromBlockPos(highlightedPos, MC.level);
            if (resourceBlock != null && resourceBlock.resourceValue > 1 && !BuildingUtils.isPosInsideAnyBuilding(true, highlightedPos)) {
                MyRenderer.drawBlockOutline(poseStack, highlightedPos, 1);
            }
        }

        /*var worldResources = WorldApi.getSingleton().resources;
        List<ApiResource> resources;
        synchronized(worldResources) {
            resources = worldResources.values().stream().distinct().toList();
        }
        for (ApiResource resource : resources) {
            if(resource != highligthedResource) {
                AABB box = new AABB(resource.getMinPos(), resource.getMaxPos());
                MyRenderer.drawLineBoxOutlineOnly(poseStack, box, 1, 1, 0, 0.125f, false);
            }
        }*/
    }

    private static final ResourceLocation FoodIcon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/wheat.png");
    private static final ResourceLocation WoodIcon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/wood.png");
    private static final ResourceLocation OreIcon = new ResourceLocation(ReignOfNether.MOD_ID, "textures/icons/items/iron_ore.png");

    public static ResourceLocation getResourceIcon(ResourceName resource) {
        return switch (resource) {
            case FOOD -> FoodIcon;
            case WOOD -> WoodIcon;
            case ORE -> OreIcon;
            default -> null;
        };
    }
}
