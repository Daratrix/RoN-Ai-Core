package com.daratrix.ronapi.ai.cursor;

import com.daratrix.ronapi.RonApi;
import com.daratrix.ronapi.ai.controller.AiController;
import com.daratrix.ronapi.ai.hud.buttons.AiStartButtons;
import com.daratrix.ronapi.ai.player.AiPlayerServerboundPacket;
import com.daratrix.ronapi.ai.registers.AiGameRuleRegister;
import com.daratrix.ronapi.ai.scripts.MonsterScript;
import com.daratrix.ronapi.ai.scripts.PiglinScript;
import com.daratrix.ronapi.ai.scripts.VillagerScript;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.HudClientEvents;
import com.solegendary.reignofnether.minimap.MinimapClientEvents;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

public class AiCursorClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final int iconSize = 14;

    private static UnitAction leftClickAction = null;

    public static UnitAction getLeftClickAction() {
        return leftClickAction;
    }

    public static void setLeftClickAction(UnitAction actionName) {
        System.out.println(RonApi.MOD_ID + ">AiCursorClientEvents.setLeftClickAction: " + actionName.name());
        if (actionName == UnitAction.STARTRTS_VILLAGERS
                || actionName == UnitAction.STARTRTS_MONSTERS
                || actionName == UnitAction.STARTRTS_PIGLINS) {
            leftClickAction = actionName;
        } else {
            leftClickAction = null;
        }
    }

    private static final ResourceLocation TEXTURE_CURSOR = new ResourceLocation("reignofnether", "textures/cursors/customcursor.png");
    private static final ResourceLocation TEXTURE_CROSS = new ResourceLocation("reignofnether", "textures/cursors/customcursor_cross.png");

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
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

        GuiGraphics guiGraphics = evt.getGuiGraphics();
        var mouseX = evt.getMouseX();
        var mouseY = evt.getMouseY();

        if (leftClickAction != null) {
            var iconX = mouseX + iconSize;
            var iconY = mouseY < MC.screen.height / 2 ? mouseY - iconSize + 20 : mouseY - iconSize;

            MyRenderer.renderIcon(guiGraphics, getFactionIcon(leftClickAction), iconX, iconY, iconSize);
        }

        if (!AiGameRuleRegister.showDebug(MC.getSingleplayerServer())) {
            return;
        }

        if (HudClientEvents.hudSelectedEntity != null) {
            var unit = WorldApi.getSingleton().units.getOrDefault(HudClientEvents.hudSelectedEntity, null);
            if (unit != null) {
                var orderId = unit.getCurrentOrderId();
                var lines = new ArrayList<FormattedCharSequence>();
                lines.add(FormattedCharSequence.forward(unit.getX() + "x, " + unit.getZ() + "y", Style.EMPTY));
                lines.add(FormattedCharSequence.forward(TypeIds.toItemName(orderId) + "(" + orderId + ")", Style.EMPTY));
                var player = WorldApi.getSingleton().players.getOrDefault(unit.getOwnerName(), null);
                var controller = AiController.controllers.getOrDefault(unit.getOwnerName(), null);
                if (player != null && controller != null) {
                    var listName = controller.getWorkerListName(unit);
                    if (listName != null) {
                        lines.add(FormattedCharSequence.forward(listName, Style.EMPTY));
                    }
                    var listName2 = controller.getArmyListName(unit);
                    if (listName2 != null) {
                        lines.add(FormattedCharSequence.forward(listName2, Style.EMPTY));
                    }
                }

                MyRenderer.renderTooltip(guiGraphics, lines, mouseX, mouseY);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (MC.level == null || !AiGameRuleRegister.showDebug(MC.getSingleplayerServer())) {
            return;
        }

        if (leftClickAction != null) {
            PoseStack poseStack = evt.getPoseStack();
            var highlightedPos = CursorClientEvents.getPreselectedBlockPos();
            MyRenderer.drawBlockFace(poseStack, Direction.UP, highlightedPos, 1, 1, 1, 1);
        }
    }

    public static ResourceLocation getFactionIcon(UnitAction action) {
        return switch (action) {
            case STARTRTS_VILLAGERS -> AiStartButtons.villagerStartButton.iconResource;
            case STARTRTS_MONSTERS -> AiStartButtons.monsterStartButton.iconResource;
            case STARTRTS_PIGLINS -> AiStartButtons.piglinStartButton.iconResource;
            default -> null;
        };
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Post evt) {
        System.out.println(RonApi.MOD_ID + ">AiCursorClientEvents.onMouseClick");
        if (!OrthoviewClientEvents.isEnabled()
                || MinimapClientEvents.isPointInsideMinimap(evt.getMouseX(), evt.getMouseY())
                || HudClientEvents.isMouseOverAnyButtonOrHud()
                || leftClickAction == null)
            return;

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1) {
            System.out.println(RonApi.MOD_ID + ">AiCursorClientEvents.onMouseClick GLFW_MOUSE_BUTTON_1");
            Vector3d worldPos = CursorClientEvents.getCursorWorldPos();
            Vec3 pos = new Vec3(worldPos.x, worldPos.y, worldPos.z);
            if (leftClickAction == UnitAction.STARTRTS_VILLAGERS) {
                System.out.println(RonApi.MOD_ID + ">AiCursorClientEvents.onMouseClick STARTRTS_VILLAGERS");
                AiPlayerServerboundPacket.startRTSBot(MC.level, Faction.VILLAGERS, VillagerScript.name, (int) worldPos.x, (int) worldPos.y, (int) worldPos.z);
            } else if (leftClickAction == UnitAction.STARTRTS_MONSTERS) {
                System.out.println(RonApi.MOD_ID + ">AiCursorClientEvents.onMouseClick STARTRTS_MONSTERS");
                AiPlayerServerboundPacket.startRTSBot(MC.level, Faction.MONSTERS, MonsterScript.name, (int) worldPos.x, (int) worldPos.y, (int) worldPos.z);
            } else if (leftClickAction == UnitAction.STARTRTS_PIGLINS) {
                System.out.println(RonApi.MOD_ID + ">AiCursorClientEvents.onMouseClick STARTRTS_PIGLINS");
                AiPlayerServerboundPacket.startRTSBot(MC.level, Faction.PIGLINS, PiglinScript.name, (int) worldPos.x, (int) worldPos.y, (int) worldPos.z);
            }
            leftClickAction = null;
        }

        if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2) {
            leftClickAction = null;
        }
    }
}
