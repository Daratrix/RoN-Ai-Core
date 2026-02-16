package com.daratrix.ronapi.ai.controller.interfaces;

import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.interfaces.IHero;
import com.daratrix.ronapi.models.interfaces.IPlayer;
import com.daratrix.ronapi.models.interfaces.IPlayerWidget;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.function.Consumer;

public interface IAiLogic {
    public String getName();

    public Faction getFaction();

    public void setPriorities(Level level, IAiPlayer player, IAiControllerPriorities priorities);

    default int getWorkerTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Villager;
            case MONSTERS -> TypeIds.Monsters.Villager;
            case PIGLINS -> TypeIds.Piglins.Grunt;
            default -> 0;
        };
    }

    default int getCapitolTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.TownCentre;
            case MONSTERS -> TypeIds.Monsters.Mausoleum;
            case PIGLINS -> TypeIds.Piglins.MainPortal;
            default -> 0;
        };
    }

    default int getFarmTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Farm;
            case MONSTERS -> TypeIds.Monsters.Farm;
            case PIGLINS -> TypeIds.Piglins.Farm;
            default -> 0;
        };
    }

    default int getAltarTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Altar;
            case MONSTERS -> TypeIds.Monsters.Altar;
            case PIGLINS -> TypeIds.Piglins.Altar;
            default -> 0;
        };
    }

    default int getTowerTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Watchtower;
            case MONSTERS -> TypeIds.Monsters.Watchtower;
            case PIGLINS -> TypeIds.Piglins.Bastion;
            default -> 0;
        };
    }

    default int getCastleTypeId() {
        return switch (this.getFaction()) {
            case VILLAGERS -> TypeIds.Villagers.Castle;
            case MONSTERS -> TypeIds.Monsters.Stronghold;
            case PIGLINS -> TypeIds.Piglins.Fortress;
            default -> 0;
        };
    }

    // indicate how many structures the AI can build at the same time
    default int getConcurrentBuildingLimit(IAiPlayer player) {
        return (int) 1 + player.countDone(this.getWorkerTypeId()) / 8;
    }

    // indicate how many workers can be pulled away from tasks to build new structures
    default int getConcurrentBuilderLimit(IAiPlayer player) {
        if (player.countDone(this.getCapitolTypeId()) == 0) {
            return this.getMaxPopulation(); // allow all available workers to build the TC
        }

        return (int) 1 + player.countDone(this.getWorkerTypeId()) / 8;
    }

    default boolean useWarpten() {
        return false;
    }

    default boolean useGreedisgood() {
        return false;
    }

    void setMaxPopulation(int maxPopulation);

    int getMaxPopulation();

    public Consumer<IPlayerWidget> getMicroLogic(int typeId);

    public Consumer<IHero> getHeroSkillLogic(int typeId);

    default boolean shouldSurrender(Level level, IAiPlayer player, IAiControllerPriorities priorities) {
        var workers = player.countDone(this.getWorkerTypeId());
        if (workers < 3) {
            return true; // not enough workers to do anything, doesn't matter if we have the resources or infrastructure
        }

        var capitolId = this.getCapitolTypeId();
        var capitols = player.count(this.getCapitolTypeId());

        if (capitols == 0 && workers < 10 && !player.canAfford(capitolId)) {
            return true; // no hope to grow economy again
        }

        var army = player.getArmyPop();
        var smallestEnemyArmy = WorldApi.getPlayerEnemies(player).map(IPlayer::getArmyPop).min(Integer::compare).orElse(0);
        if (workers < smallestEnemyArmy * 0.5 && army <= 0.15f * smallestEnemyArmy) {
            return true; // army is too small to do anything
        }

        return false;
    }

    public abstract class AbstractAiLogic implements IAiLogic {
        protected int maxPopulation = 150;
        protected HashMap<Integer, Consumer<IPlayerWidget>> microLogics = new HashMap<>();
        protected HashMap<Integer, Consumer<IHero>> heroSkillLogics = new HashMap<>();

        public void setMaxPopulation(int maxPopulation) {
            this.maxPopulation = maxPopulation;
        }

        public int getMaxPopulation() {
            return this.maxPopulation;
        }

        @Override
        public Consumer<IPlayerWidget> getMicroLogic(int typeId) {
            return this.microLogics.getOrDefault(typeId, null);
        }

        @Override
        public Consumer<IHero> getHeroSkillLogic(int typeId) {
            return this.heroSkillLogics.getOrDefault(typeId, null);
        }
    }
}
