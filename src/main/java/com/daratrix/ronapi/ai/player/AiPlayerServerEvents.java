package com.daratrix.ronapi.ai.player;

import com.daratrix.ronapi.ai.controller.AiController;
import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.registers.AiGameRuleRegister;
import com.daratrix.ronapi.apis.WorldApi;
import com.solegendary.reignofnether.player.PlayerClientboundPacket;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.research.ResearchClientboundPacket;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.util.Faction;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;

public class AiPlayerServerEvents {

    public static void startRTSBot(MinecraftServer server, String name, Vec3 pos, Faction faction) {
        var id = getNextBotId();
        var aiPlayerName = name + id;
        PlayerServerEvents.startRTSBot(aiPlayerName, pos, faction);
        var rtsPlayer = PlayerServerEvents.rtsPlayers.stream().filter(p -> p.id == id).findFirst().get();
        PlayerClientboundPacket.addRTSPlayer(rtsPlayer.name, rtsPlayer.faction, (long) rtsPlayer.id, rtsPlayer.startPosColorId);
        System.out.println("Creating new AI agent " + rtsPlayer.name);
        var controller = createAiPlayer(server, rtsPlayer, name);
        WorldApi.getSingleton().track(server, (AiPlayer) controller.player);

        if (AiGameRuleRegister.forceGreedisgood(server) || controller.logic.useGreedisgood()) {
            // extra resources for faster development
            ResourcesServerEvents.addSubtractResources(new Resources(rtsPlayer.name, 50000, 50000, 50000));
        }
    }

    public static AiPlayer restoreAiPlayer(MinecraftServer server, RTSPlayer rtsPlayer) {
        System.out.println("Restoring AI agent " + rtsPlayer.name);
        var logicName = rtsPlayer.name.substring(0, rtsPlayer.name.lastIndexOf("-"));
        return (AiPlayer) createAiPlayer(server, rtsPlayer, logicName).player;
    }

    public static AiController createAiPlayer(MinecraftServer server, RTSPlayer rtsPlayer, String logicName) {
        var aiPlayer = new AiPlayer(rtsPlayer);
        var controller = startAiController(aiPlayer, logicName);

        if (AiGameRuleRegister.forceWarpten(server) || controller.logic.useWarpten()) {
            // warpten for faster development
            ResearchServerEvents.addCheat(rtsPlayer.name, "warpten");
            ResearchClientboundPacket.addCheat(rtsPlayer.name, "warpten");
        }

        return controller;
    }

    public static AiController startAiController(IAiPlayer aiPlayer, String logicName) {
        System.out.println("Creating AI controller using logic " + logicName);
        var logic = AiLogics.get(logicName);
        return new AiController(aiPlayer, logic);
    }

    public static int getNextBotId() {
        int minId = 0;
        if (!PlayerServerEvents.rtsPlayers.isEmpty()) {
            minId = (Integer) Collections.min(PlayerServerEvents.rtsPlayers.stream().map((r) -> {
                return r.id;
            }).toList());
        }

        if (minId >= 0) {
            return -1;
        } else {
            return minId - 1;
        }
    }
}
