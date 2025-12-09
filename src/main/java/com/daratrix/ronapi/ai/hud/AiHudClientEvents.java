package com.daratrix.ronapi.ai.hud;

import com.daratrix.ronapi.ai.registers.AiGameRuleRegister;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiPlayerBase;
import com.daratrix.ronapi.models.ApiResource;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IPlayer;
import com.daratrix.ronapi.models.interfaces.IResource;
import com.mojang.blaze3d.vertex.PoseStack;
import com.solegendary.reignofnether.cursor.CursorClientEvents;
import com.solegendary.reignofnether.guiscreen.TopdownGui;
import com.solegendary.reignofnether.hud.Button;
import com.daratrix.ronapi.ai.hud.buttons.AiStartButtons;
import com.solegendary.reignofnether.orthoview.OrthoviewClientEvents;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.util.MyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;

import static com.solegendary.reignofnether.hud.buttons.StartButtons.ICON_SIZE;

public class AiHudClientEvents {

    private static final Minecraft MC = Minecraft.getInstance();

    // buttons which are rendered at the moment in RenderEvent
    private static final ArrayList<Button> renderedButtons = new ArrayList<>();

    public static int mouseX = 0;
    public static int mouseY = 0;

    @SubscribeEvent
    public static void onDrawScreen(ScreenEvent.Render.Post evt) {
        if (!OrthoviewClientEvents.isEnabled() || !(evt.getScreen() instanceof TopdownGui))
            return;
        if (MC.level == null)
            return;

        renderedButtons.clear();

        mouseX = evt.getMouseX();
        mouseY = evt.getMouseY();

        // create all the unit buttons for this frame
        int screenWidth = MC.getWindow().getGuiScaledWidth();

        // ------------------------------
        // Start buttons (spectator only)
        // ------------------------------
        if (!PlayerClientEvents.rtsLocked) {
            if (!AiStartButtons.villagerStartButton.isHidden.get()) {
                AiStartButtons.villagerStartButton.render(evt.getGuiGraphics(),
                        screenWidth - (ICON_SIZE * 6),
                        ICON_SIZE * 2 + ICON_SIZE / 2,
                        mouseX, mouseY);
                renderedButtons.add(AiStartButtons.villagerStartButton);
            }
            if (!AiStartButtons.monsterStartButton.isHidden.get()) {
                AiStartButtons.monsterStartButton.render(evt.getGuiGraphics(),
                        (int) (screenWidth - (ICON_SIZE * 4f)),
                        ICON_SIZE * 2 + ICON_SIZE / 2,
                        mouseX, mouseY);
                renderedButtons.add(AiStartButtons.monsterStartButton);
            }
            if (!AiStartButtons.piglinStartButton.isHidden.get()) {
                AiStartButtons.piglinStartButton.render(evt.getGuiGraphics(),
                        screenWidth - (ICON_SIZE * 2),
                        ICON_SIZE * 2 + ICON_SIZE / 2,
                        mouseX, mouseY);
                renderedButtons.add(AiStartButtons.piglinStartButton);
            }
        }

        // ------------------------------------------------------
        // Button tooltips (has to be rendered last to be on top)
        // ------------------------------------------------------
        for (Button button : renderedButtons)
            if (button.isMouseOver(mouseX, mouseY))
                button.renderTooltip(evt.getGuiGraphics(), mouseX, mouseY);

    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent evt) {
        if (!OrthoviewClientEvents.isEnabled())
            return;
        if (MC.level == null || MC.noRender)
            return;

        if (!AiGameRuleRegister.showDebug(MC.getSingleplayerServer())) {
            return;
        }

        PoseStack poseStack = evt.getPoseStack();
        var highlightedPos = CursorClientEvents.getPreselectedBlockPos();
        var world = WorldApi.getSingleton();

        //for (IBuilding b : WorldApi.getSingleton().buildings.values()) {
        //    MyRenderer.drawLineBoxOutlineOnly(poseStack, b.getBoundingBox(), 1, 1, 1, 1, false);
        //}

        for (IPlayer p : WorldApi.getSingleton().players.values()) {
            p.getBasesFiltered(b -> true).forEach(b -> {
                if (b.getThreatPower() > 10) {
                    MyRenderer.drawLineBoxOutlineOnly(poseStack, b.getBoundingBox(), 1, 0, 0, 1, false);
                } else if (b.getThreatPower() > 0) {
                    MyRenderer.drawLineBoxOutlineOnly(poseStack, b.getBoundingBox(), 1, 1, 0, 0.66f, false);
                } else {
                    MyRenderer.drawLineBoxOutlineOnly(poseStack, b.getBoundingBox(), 1, 1, 1, 0.33f, false);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onMousePress(ScreenEvent.MouseButtonPressed.Post evt) {
        for (Button button : renderedButtons) {
            if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_1)
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), true);
            else if (evt.getButton() == GLFW.GLFW_MOUSE_BUTTON_2)
                button.checkClicked((int) evt.getMouseX(), (int) evt.getMouseY(), false);
        }
    }
}
