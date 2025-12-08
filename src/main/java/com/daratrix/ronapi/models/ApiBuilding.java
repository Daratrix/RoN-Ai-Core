/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models;

import com.daratrix.ronapi.apis.BuildingApi;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IOrder;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.models.interfaces.IWidget;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.GarrisonableBuilding;
import com.solegendary.reignofnether.building.buildings.piglins.PortalCivilian;
import com.solegendary.reignofnether.building.buildings.placements.*;
import com.solegendary.reignofnether.building.production.ProductionBuilding;
import com.solegendary.reignofnether.building.buildings.monsters.Laboratory;
import com.solegendary.reignofnether.building.buildings.piglins.AbstractPortal;
import com.solegendary.reignofnether.building.buildings.villagers.Castle;
import com.solegendary.reignofnether.building.buildings.villagers.Library;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.resources.ResourcesServerEvents;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Daratrix
 */
public class ApiBuilding implements IBuilding {

    protected final BuildingPlacement physicalBuilding;
    protected final ProductionPlacement production;
    protected final Building buildingData;
    protected final int typeId;
    private final AABB boundingBox;
    protected final boolean isCapitol;

    public ApiBuilding(BuildingPlacement building) {
        this.physicalBuilding = building;
        this.buildingData = building.getBuilding();
        this.production = this.physicalBuilding instanceof ProductionPlacement p ? p : null;
        this.typeId = TypeIds.get(this.buildingData);
        this.boundingBox = GeometryUtils.getBoundingBox(building.minCorner, building.maxCorner.offset(1, 1, 1));
        this.isCapitol = building.isCapitol;
    }

    @Override
    public List<Integer> getProductionQueue() {
        return List.of();
    }

    @Override
    public float getProductionProgress() {
        return 0;
    }

    @Override
    public BlockPos getPos() {
        return this.physicalBuilding.originPos;
    }

    @Override
    public int getPopSupplied() {
        //if(this.typeId == TypeIds.Piglins.Portal && this.hasUpgrade(TypeIds.Piglins.CivilianPortal)) {
        //    return PortalCivilian.CIVILIIAN_PORTAL_POPULATION_SUPPLY;
        //}

        return this.physicalBuilding.getBuilding().cost.population;
    }

    @Override
    public boolean isDone() {
        return this.physicalBuilding.isBuilt;
    }

    @Override
    public boolean isUnderConstruction() {
        return !this.physicalBuilding.isBuilt;
    }

    @Override
    public String getOwnerName() {
        return this.physicalBuilding.ownerName;
    }

    @Override
    public void setOwnerByName(String ownerName) {
        this.physicalBuilding.ownerName = ownerName;
    }

    @Override
    public int getCurrentOrderId() {
        if (this.production == null) {
            return 0;
        }

        var queue = this.production.productionQueue;
        if (queue == null || queue.isEmpty()) {
            return 0;
        }

        return TypeIds.get(queue.get(0).item);
    }

    @Override
    public Object getCurrentOrderTarget() {
        return null;
    }

    private static final ArrayList<WorkerUnit> countBuildersCollection = new ArrayList<>();

    @Override
    public int countBuilders() {
        countBuildersCollection.clear();
        WorldApi.getSingleton().players.get(this.physicalBuilding.ownerName)
                .getUnitsFiltered(u -> u.isWorker() && u.isAlive())
                .map(IUnit::getWorkerUnit)
                .collect(Collectors.toCollection(() -> countBuildersCollection));

        var count = 0;

        for (WorkerUnit w : countBuildersCollection) {
            var repair = w.getBuildRepairGoal();
            var target = repair.getBuildingTarget();
            if (target == this.physicalBuilding) {
                ++count;
            }
        }

        return count;
    }

    @Override
    public boolean isCapitol() {
        return this.isCapitol;
    }

    @Override
    public boolean canBeGarrisoned() {
        return this.physicalBuilding instanceof GarrisonableBuilding;
    }

    @Override
    public boolean hasUpgrade(int upgradeId) {
        if (this.typeId == TypeIds.Villagers.Castle) {
            return upgradeId == TypeIds.Villagers.OfficersQuarters
                    && this.physicalBuilding.getUpgradeLevel() > 0; // expensive call...
        }

        if (this.typeId == TypeIds.Villagers.Library) {
            return upgradeId == TypeIds.Villagers.GrandLibrary
                    && this.physicalBuilding.getUpgradeLevel() > 0; // expensive call...
        }

        if (this.typeId == TypeIds.Monsters.Laboratory) {
            return upgradeId == TypeIds.Monsters.LightningRod
                    && this.physicalBuilding.getUpgradeLevel() > 0; // expensive call...
        }

        if (this.typeId == TypeIds.Piglins.Portal) {
            var portalType = ((PortalPlacement) this.physicalBuilding).getPortalType();
            return switch (portalType) {
                case CIVILIAN -> upgradeId == TypeIds.Piglins.CivilianPortal;
                case MILITARY -> upgradeId == TypeIds.Piglins.MilitaryPortal;
                case TRANSPORT -> upgradeId == TypeIds.Piglins.TransportPortal;
                default -> false;
            };
        }

        return false;
    }

    @Override
    public boolean isUpgraded() {
        return this.physicalBuilding.getUpgradeLevel() > 0;
    }

    @Override
    public List<IOrder> getOrderQueue() {
        return List.of();
    }

    @Override
    public boolean isIdle() {
        return this.production == null || this.production.productionQueue.isEmpty();
    }

    @Override
    public Stream<Ability> getAbilities() {
        return this.physicalBuilding.getAbilities().stream();
    }

    @Override
    public <T extends Ability> void setAbilityAutocast(Class<T> abilityClass, boolean on) {
        getAbility(abilityClass).setAutocast(on, this.physicalBuilding);
    }

    @Override
    public <T extends Ability> boolean isAbilityAutocasting(Class<T> abilityClass) {
        return getAbility(abilityClass).isAutocasting(this.physicalBuilding);
    }

    @Override
    public <T extends Ability> boolean isAbilityOffCooldown(Class<T> abilityClass) {
        return getAbility(abilityClass).isOffCooldown(this.physicalBuilding);
    }

    @Override
    public <T extends HeroAbility> int getAbilityRank(Class<T> abilityClass) {
        return 0; // buildings don't have hero abilities, as of now
    }

    @Override
    public boolean issuePointOrder(int x, int y, int z, int orderId) {
        return false;
    }

    @Override
    public boolean issuePointOrder(BlockPos pos, int orderId) {
        return false;
    }

    @Override
    public boolean issueWidgetOrder(IWidget target, int orderId) {
        return false;
    }

    @Override
    public boolean issueWidgetOrder(LivingEntity target, int orderId) {
        return false;
    }

    @Override
    public boolean issueWidgetOrder(BuildingPlacement target, int orderId) {
        return false;
    }

    @Override
    public boolean issueOrder(int typeId) {

        return false;
    }

    @Override
    public boolean issueTrainOrder(int typeId) {
        if (this.production == null) {
            return false;
        }

        var production = TypeIds.toProductionItem(typeId);
        if (production == null) {
            return false;
        }

        return this.production.startProductionItem(production);
    }

    @Override
    public boolean issueReviveOrder(int typeId) {
        if (this.production == null) {
            return false;
        }

        var production = TypeIds.toReviveItem(typeId);
        if (production == null) {
            return false;
        }

        return this.production.startProductionItem(production);
    }

    @Override
    public boolean issueResearchOrder(int typeId) {
        return this.issueTrainOrder(typeId);
    }

    @Override
    public boolean issueUpgradeOrder(int typeId) {
        return this.issueTrainOrder(typeId);
    }

    @Override
    public Building getBuildingData() {
        return this.buildingData;
    }

    @Override
    public BuildingPlacement getBuilding() {
        return this.physicalBuilding;
    }

    @Override
    public boolean clearOrder(boolean clearQueue) {
        return false;
    }

    @Override
    public int getMinX() {
        return this.physicalBuilding.minCorner.getX();
    }

    @Override
    public int getMaxX() {
        return this.physicalBuilding.maxCorner.getX();
    }

    @Override
    public int getMinY() {
        return this.physicalBuilding.minCorner.getY();
    }

    @Override
    public int getMaxY() {
        return this.physicalBuilding.maxCorner.getY();
    }

    @Override
    public int getMinZ() {
        return this.physicalBuilding.minCorner.getZ();
    }

    @Override
    public int getMaxZ() {
        return this.physicalBuilding.maxCorner.getZ();
    }

    @Override
    public BlockPos getMinPos() {
        return this.physicalBuilding.minCorner;
    }

    @Override
    public BlockPos getMaxPos() {
        return this.physicalBuilding.maxCorner;
    }

    @Override
    public int getBlockCount() {
        return 0;
    }

    @Override
    public Stream<BlockPos> getBlocks() {
        return Stream.empty();
    }

    @Override
    public AABB getBoundingBox() {
        return this.boundingBox;
    }

    @Override
    public void destroy() {
        //this.building.destroy(this.building.getLevel());
    }

    @Override
    public String getName() {
        return this.buildingData.name;
    }

    @Override
    public int getHealth() {
        return this.physicalBuilding.getHealth();
    }

    @Override
    public int getMaxHealth() {
        return this.physicalBuilding.getMaxHealth();
    }

    @Override
    public boolean isAlive() {
        return this.physicalBuilding.getHealth() > 0;
    }

    @Override
    public boolean isDead() {
        return this.physicalBuilding.getHealth() <= 0;
    }

    @Override
    public float getX() {
        return this.physicalBuilding.centrePos.getX();
    }

    @Override
    public float getY() {
        return this.physicalBuilding.centrePos.getY();
    }

    @Override
    public float getZ() {
        return this.physicalBuilding.centrePos.getZ();
    }

    @Override
    public void kill() {

    }

    @Override
    public int getTypeId() {
        return this.typeId;
    }

    @Override
    public boolean is(int typeId) {
        return this.typeId == typeId || this.hasUpgrade(typeId);
    }
}
