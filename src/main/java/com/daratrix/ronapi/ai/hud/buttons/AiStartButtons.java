package com.daratrix.ronapi.ai.hud.buttons;

import com.daratrix.ronapi.ai.cursor.AiCursorClientEvents;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.player.PlayerClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialClientEvents;
import com.solegendary.reignofnether.tutorial.TutorialStage;
import com.solegendary.reignofnether.unit.UnitAction;
import java.util.List;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import static com.solegendary.reignofnether.hud.buttons.StartButtons.ICON_SIZE;

public class AiStartButtons {

    public static Button villagerStartButton = new Button(
            "[CPU] Villagers",
            ICON_SIZE,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/villager.png"),
            (Keybinding) null,
            () -> AiCursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_VILLAGERS,
            () -> !TutorialClientEvents.isAtOrPastStage(TutorialStage.PLACE_WORKERS_B) || !PlayerClientEvents.canStartRTS,
            () -> true,
            () -> AiCursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_VILLAGERS),
            () -> { },
            List.of(
                    FormattedCharSequence.forward("Add an AI controlled Villager faction.", Style.EMPTY),
                    FormattedCharSequence.forward("Spawns 3 Villagers to the target location.", Style.EMPTY)
            )
    );

    public static Button monsterStartButton = new Button(
            "[CPU] Monsters",
            ICON_SIZE,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/creeper.png"),
            (Keybinding) null,
            () -> AiCursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_MONSTERS,
            () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS,
            () -> !TutorialClientEvents.isEnabled(),
            () -> AiCursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_MONSTERS),
            () -> { },
            List.of(
                    FormattedCharSequence.forward("Add an AI controlled Monster faction.", Style.EMPTY),
                    FormattedCharSequence.forward("Spawns 3 zombie Villagers at the target location.", Style.EMPTY)
            )
    );
    public static Button piglinStartButton= new Button(
            "[CPU] Piglins",
            ICON_SIZE,
            new ResourceLocation(ReignOfNether.MOD_ID, "textures/mobheads/grunt.png"),
            (Keybinding) null,
            () -> AiCursorClientEvents.getLeftClickAction() == UnitAction.STARTRTS_PIGLINS,
            () -> TutorialClientEvents.isEnabled() || !PlayerClientEvents.canStartRTS,
            () -> !TutorialClientEvents.isEnabled(),
            () -> AiCursorClientEvents.setLeftClickAction(UnitAction.STARTRTS_PIGLINS),
            () -> { },
            List.of(
                    FormattedCharSequence.forward("Add an AI controlled Piglin faction.", Style.EMPTY),
                    FormattedCharSequence.forward("Spawns in 3 piglin Grunts to the target location.", Style.EMPTY)
            )
    );
}
