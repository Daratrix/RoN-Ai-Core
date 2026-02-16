package com.daratrix.ronapi.ai.controller;

import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.solegendary.reignofnether.faction.Faction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class AiLogics {

    public final static Map<String, IAiLogic> aiLogics = new HashMap<>();
    public final static Map<Faction, ArrayList<String>> factionAiLogicNames;

    static {
        factionAiLogicNames = new HashMap<>();
        for (Faction faction : Faction.values()) {
            factionAiLogicNames.put(faction, new ArrayList<>());
        }
    }

    public static void registerAiLogic(IAiLogic logic) {
        aiLogics.put(logic.getName(), logic);
        factionAiLogicNames.get(logic.getFaction()).add(logic.getName());
    }

    public static IAiLogic get(String name) {
        return aiLogics.get(name);
    }

    public static Stream<String> get(Faction faction) {
        return factionAiLogicNames.get(faction).stream();
    }
}
