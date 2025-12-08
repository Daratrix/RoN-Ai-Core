package com.daratrix.ronapi.models;

import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IPlayer;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.unit.UnitClientEvents;
import com.solegendary.reignofnether.player.RTSPlayer;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ApiPlayer implements IPlayer {

    public ApiPlayer(RTSPlayer player) {
        this.player = player;
        this.name = player.name;
        this.isBot = player.isBot();
        this.resources = ResourcesServerEvents.resourcesList.stream().filter(r -> r.ownerName.equals(this.name)).findFirst().orElse(null);
    }

    protected final RTSPlayer player;
    protected Resources resources;
    protected final String name;
    protected final boolean isBot;

    protected List<IUnit> units = new ArrayList<>();
    protected List<IBuilding> buildings = new ArrayList<>();
    protected List<ApiPlayerBase> bases = new ArrayList<>();

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int countUnit(String name) {
        return 0;
    }

    @Override
    public int countBuilding(String name) {
        return 0;
    }

    @Override
    public int countResearch(String name) {
        return 0;
    }

    @Override
    public boolean hasResearch(int researchId) {
        return ResearchServerEvents.playerHasResearch(this.name, TypeIds.toProductionItem(researchId));
    }

    @Override
    public int getFood() {
        if (this.resources == null) {
            this.resources = ResourcesServerEvents.resourcesList.stream().filter(r -> r.ownerName.equals(this.name)).findFirst().orElse(null);
            if (this.resources == null) {
                return 0;
            }
        }
        return this.resources.food;
    }

    @Override
    public int getWood() {
        if (this.resources == null) {
            this.resources = ResourcesServerEvents.resourcesList.stream().filter(r -> r.ownerName.equals(this.name)).findFirst().orElse(null);
            if (this.resources == null) {
                return 0;
            }
        }
        return this.resources.wood;
    }

    @Override
    public int getOre() {
        if (this.resources == null) {
            this.resources = ResourcesServerEvents.resourcesList.stream().filter(r -> r.ownerName.equals(this.name)).findFirst().orElse(null);
            if (this.resources == null) {
                return 0;
            }
        }
        return this.resources.ore;
    }

    @Override
    public int getPopUsed() {
        return this.units.stream().filter(u -> u.isAlive()).map(u -> u.getPopUsed()).reduce((a, b) -> a + b).orElse(0);
    }

    @Override
    public int getPopCap() {
        return this.buildings.stream().filter(b -> b.isAlive() && b.isDone()).map(b -> b.getPopSupplied()).reduce((a, b) -> a + b).orElse(0);
    }

    @Override
    public int getArmyPop() {
        return this.units.stream().filter(u -> u.isAlive() && !u.isWorker()).map(IUnit::getPopUsed).reduce(0, Integer::sum);
    }

    @Override
    public int getWorkerPop() {
        return this.units.stream().filter(u -> u.isAlive() && u.isWorker()).map(IUnit::getPopUsed).reduce(0, Integer::sum);
    }

    @Override
    public boolean canAfford(int typeId) {
        if (this.canAfford(TypeIds.toItemCost(typeId))) {
            return true;
        }

        //System.err.println("Can't afford " + TypeIds.toItemName(typeId));
        return false;
    }

    @Override
    public boolean canAfford(ResourceCost cost) {
        if (cost == null) {
            //System.err.println("No cost");
            return true;
        }

        if (this.resources == null) {
            this.resources = ResourcesServerEvents.resourcesList.stream().filter(r -> r.ownerName.equals(this.name)).findFirst().orElse(null);
            if (this.resources == null) {
                return false;
            }
        }

        return this.resources.food >= cost.food
                && this.resources.wood >= cost.wood
                && this.resources.ore >= cost.ore;
    }

    @Override
    public boolean isDefeated() {
        return !PlayerServerEvents.rtsPlayers.contains(this.player);
    }

    @Override
    public Stream<IUnit> getUnitsFiltered(Predicate<IUnit> predicate) {
        return this.units.stream().filter(predicate);
    }

    @Override
    public Stream<IBuilding> getBuildingsFiltered(Predicate<IBuilding> predicate) {
        return this.buildings.stream().filter(predicate);
    }

    @Override
    public Stream<ApiPlayerBase> getBasesFiltered(Predicate<ApiPlayerBase> predicate) {
        return this.bases.stream().filter(predicate);
    }

    public void reset() {
        units.clear();
        buildings.clear();
    }

    public boolean track(IUnit unit) {
        if (!units.contains(unit)) {
            return units.add(unit);
        }
        return false;
    }

    public boolean track(IBuilding building) {
        if (!buildings.contains(building)) {
            var base = this.getBasesFiltered(b -> GeometryUtils.isWithinDistance(b, building, 20)).findFirst().orElse(null);
            if (base == null) {
                this.bases.add(new ApiPlayerBase(building));
            } else {
                base.track(building);
            }
            return buildings.add(building);
        }
        return false;
    }

    public boolean untrack(IBuilding building) {
        if (buildings.contains(building)) {
            var base = this.getBasesFiltered(b -> GeometryUtils.isWithinDistance(b, building, 20)).findFirst().orElse(null);
            if (base != null && !base.untrack(building)) {
                this.bases.remove(base); // if untrack returns false, the base is empty and should be discarded
            }
            return this.buildings.remove(building);
        }
        return true;
    }

    public boolean untrack(IUnit unit) {
        return this.units.remove(unit);
    }
}
