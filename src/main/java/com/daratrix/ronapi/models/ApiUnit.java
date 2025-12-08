/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models;

import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.interfaces.IBuilding;
import com.daratrix.ronapi.models.interfaces.IOrder;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.models.interfaces.IWidget;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.resources.*;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.goals.MoveToTargetBlockGoal;
import com.solegendary.reignofnether.unit.goals.RangedAttackBuildingGoal;
import com.solegendary.reignofnether.unit.interfaces.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Rotation;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Daratrix
 */
public class ApiUnit implements IUnit {
    protected final Unit unit;
    protected final WorkerUnit worker;
    protected final HeroUnit hero;
    protected final AttackerUnit attacker;

    @Override
    public int getMana() {
        if (this.hero == null) {
            return 0;
        }

        return (int) this.hero.getMana();
    }

    @Override
    public int getMaxMana() {
        if (this.hero == null) {
            return 0;
        }

        return (int) this.hero.getMaxMana();
    }

    protected final LivingEntity entity;
    protected final Mob mob;
    protected final int typeId;

    public ApiUnit(Unit unit) {
        this.unit = unit;
        this.attacker = unit instanceof AttackerUnit a ? a : null;
        this.worker = unit instanceof WorkerUnit w ? w : null;
        this.hero = unit instanceof HeroUnit h ? h : null;
        this.entity = unit instanceof LivingEntity e ? e : null;
        this.mob = unit instanceof Mob m ? m : null;

        this.typeId = TypeIds.get(unit);
    }

    @Override
    public Unit getUnit() {
        return this.unit;
    }

    @Override
    public WorkerUnit getWorkerUnit() {
        return this.worker;
    }

    @Override
    public boolean isIdle() {
        if (this.worker != null) {
            return WorkerUnit.isIdle(this.worker) || this.getCurrentOrderId() == 0;
        }

        return this.getCurrentOrderId() == 0;
    }

    @Override
    public Stream<Ability> getAbilities() {
        return this.unit.getAbilities().get().stream();
    }

    @Override
    public <T extends Ability> T getAbility(Class<T> abiltiyClass) {
        var abilities = this.unit.getAbilities().get();
        for (var ability : abilities) {
            if (abiltiyClass.isInstance(ability)) {
                return abiltiyClass.cast(ability);
            }
        }

        return null;
    }

    @Override
    public <T extends Ability> void setAbilityAutocast(Class<T> abilityClass, boolean on) {
        getAbility(abilityClass).setAutocast(on, this.unit);
    }

    @Override
    public <T extends Ability> boolean isAbilityAutocasting(Class<T> abilityClass) {
        return getAbility(abilityClass).isAutocasting(this.unit);
    }

    @Override
    public <T extends Ability> boolean isAbilityOffCooldown(Class<T> abilityClass) {
        return getAbility(abilityClass).isOffCooldown(this.unit);
    }

    @Override
    public <T extends HeroAbility> int getAbilityRank(Class<T> abilityClass) {
        return getAbility(abilityClass).getRank(this.hero);
    }

    @Override
    public String getOwnerName() {
        return this.unit.getOwnerName();
    }

    @Override
    public void setOwnerByName(String ownerName) {
        this.unit.setOwnerName(ownerName);
    }

    @Override
    public int getCurrentOrderId() {
        var returning = this.unit.getReturnResourcesGoal();
        if (returning != null) {
            var returnTarget = returning.getBuildingTarget();
            if (returnTarget != null) {
                return TypeIds.Orders.Return;
            }
        }

        if (this.worker != null) {
            var repairing = this.worker.getBuildRepairGoal();
            if (repairing.getBuildingTarget() != null) {
                return TypeIds.Orders.Repair;
            }

            var gathering = this.worker.getGatherResourceGoal();
            var farm = gathering.data.targetFarm;
            if (farm != null) {
                return TypeIds.Resources.FoodFarm;
            }

            var resource = gathering.getTargetResourceName();
            var targetBlock = gathering.data.gatherTarget;
            if (targetBlock != null && resource != ResourceName.NONE) {
                return TypeIds.get(resource);
            }
        }

        if (this.attacker != null) {
            var attackMoveTarget = this.attacker.getAttackMoveTarget();
            if (attackMoveTarget != null) {
                return TypeIds.Orders.AttackMove;
            }

            var target = this.mob != null
                    ? this.mob.getTarget()
                    : null;

            if (target != null) {
                return TypeIds.Orders.AttackUnit;
            }

            if (this.attacker.canAttackBuildings()) {
                var goal = this.attacker.getAttackBuildingGoal();
                if (goal instanceof MeleeAttackBuildingGoal melee) {
                    var meleeTarget = melee.getBuildingTarget();
                    if (meleeTarget != null) {
                        return TypeIds.Orders.AttackBuilding;
                    }
                }

                if (goal instanceof RangedAttackBuildingGoal ranged) {
                    var rangedTarget = ranged.getBuildingTarget();
                    if (rangedTarget != null) {
                        return TypeIds.Orders.AttackBuilding;
                    }
                }
            }
        }

        if (this.unit.getFollowTarget() != null) {
            return TypeIds.Orders.Follow;
        }

        MoveToTargetBlockGoal moveToTargetBlockGoal = this.unit.getMoveGoal();
        if (moveToTargetBlockGoal != null) {
            var targetBlock = moveToTargetBlockGoal.getMoveTarget();
            if (targetBlock != null && !moveToTargetBlockGoal.isAtDestination()) {
                return TypeIds.Orders.Move;
            }
        }

        /*
        GarrisonGoal garrisonGoal = this.unit.getGarrisonGoal();
        UsePortalGoal usePortalGoal = this.unit.getUsePortalGoal();
        */
        return 0;
    }

    @Override
    public Object getCurrentOrderTarget() {
        var returning = this.unit.getReturnResourcesGoal();
        if (returning != null) {
            var returnTarget = returning.getBuildingTarget();
            if (returnTarget != null) {
                return returnTarget;
            }
        }

        if (this.worker != null) {
            var repairing = this.worker.getBuildRepairGoal();
            var repairTarget = repairing.getBuildingTarget();
            if (repairTarget != null) {
                return repairTarget;
            }

            var gathering = this.worker.getGatherResourceGoal();
            if (gathering != null) {
                var farm = gathering.data.targetFarm;
                if (farm != null) {
                    return farm;
                }

                var resource = gathering.getTargetResourceName();
                var targetBlock = gathering.data.gatherTarget;
                if (targetBlock != null && resource != ResourceName.NONE) {
                    return targetBlock;
                }
            }
        }

        if (this.attacker != null) {
            var attackMoveTarget = this.attacker.getAttackMoveTarget();
            if (attackMoveTarget != null) {
                return attackMoveTarget;
            }

            var targetEntity = this.mob != null
                    ? this.mob.getTarget()
                    : null;
            if (targetEntity != null) {
                return targetEntity;
            }

            if (this.attacker.canAttackBuildings()) {
                var goal = this.attacker.getAttackBuildingGoal();
                if (goal instanceof MeleeAttackBuildingGoal melee) {
                    var meleeTarget = melee.getBuildingTarget();
                    if (meleeTarget != null) {
                        return meleeTarget;
                    }
                }

                if (goal instanceof RangedAttackBuildingGoal ranged) {
                    var rangedTarget = ranged.getBuildingTarget();
                    if (rangedTarget != null) {
                        return rangedTarget;
                    }
                }
            }
        }

        var followTarget = this.unit.getFollowTarget();
        if (followTarget != null) {
            return followTarget;
        }

        MoveToTargetBlockGoal moveToTargetBlockGoal = this.unit.getMoveGoal();
        if (moveToTargetBlockGoal != null) {
            var targetBlock = moveToTargetBlockGoal.getMoveTarget();
            if (targetBlock != null && !moveToTargetBlockGoal.isAtDestination()) {
                return targetBlock;
            }
        }

        return null;
    }

    @Override
    public List<IOrder> getOrderQueue() {
        return List.of();
    }

    @Override
    public boolean issuePointOrder(BlockPos pos, int orderId) {
        UnitAction action = TypeIds.toUnitAction(orderId, pos);
        if (action == UnitAction.NONE) {
            return false;
        }

        UnitServerEvents.addActionItem(
                this.getOwnerName(),
                action,
                0,
                new int[]{this.entity.getId()},
                pos,
                pos
        );

        return true; // attempted, but can't check for success...
    }

    @Override
    public boolean issueWidgetOrder(IWidget target, int orderId) {
        UnitAction action = TypeIds.toUnitAction(orderId, target);
        if (action == UnitAction.NONE) {
            return false;
        }

        var targetUnit = target instanceof IUnit u ? u : null;
        var entityTargetId = targetUnit != null ? targetUnit.getId() : -1;
        var targetBuildingPos = target instanceof IBuilding b ? b.getPos() : new BlockPos(0, 0, 0);

        UnitServerEvents.addActionItem(
                this.getOwnerName(),
                action,
                entityTargetId,
                new int[]{this.entity.getId()},
                targetBuildingPos,
                targetBuildingPos
        );

        return true;
    }

    @Override
    public boolean issueWidgetOrder(LivingEntity target, int orderId) {
        UnitAction action = TypeIds.toUnitAction(orderId, target);
        if (action == UnitAction.NONE) {
            return false;
        }

        var entityTargetId = target != null ? target.getId() : -1;
        var targetBuildingPos = new BlockPos(0, 0, 0);

        UnitServerEvents.addActionItem(
                this.getOwnerName(),
                action,
                entityTargetId,
                new int[]{this.entity.getId()},
                targetBuildingPos,
                targetBuildingPos
        );

        return true;
    }

    @Override
    public boolean issueWidgetOrder(BuildingPlacement target, int orderId) {
        UnitAction action = TypeIds.toUnitAction(orderId, target);
        if (action == UnitAction.NONE) {
            return false;
        }

        var entityTargetId = -1;
        var targetBuildingPos = target != null ? target.originPos : new BlockPos(0, 0, 0);

        UnitServerEvents.addActionItem(
                this.getOwnerName(),
                action,
                entityTargetId,
                new int[]{this.entity.getId()},
                targetBuildingPos,
                targetBuildingPos
        );

        return true;
    }

    @Override
    public boolean issueOrder(int orderId) {
        UnitAction action = TypeIds.toUnitAction(orderId, null);
        if (action == UnitAction.NONE) {
            return false;
        }

        var entityTargetId = -1;
        var targetBuildingPos = new BlockPos(0, 0, 0);

        UnitServerEvents.addActionItem(
                this.getOwnerName(),
                action,
                entityTargetId,
                new int[]{this.entity.getId()},
                targetBuildingPos,
                targetBuildingPos
        );

        return true;
    }

    @Override
    public boolean issueBuildOrder(BlockPos pos, int typeId) {
        if (this.worker == null || this.entity == null || pos == null) {
            return false;
        }

        var building = TypeIds.toBuildingData(typeId);
        if (!WorldApi.playerCanAfford(this.getOwnerName(), TypeIds.toItemCost(typeId))) {
            return false;
        }

        var existingBuildingCount = BuildingServerEvents.getBuildings().size();
        BuildingServerEvents.placeBuilding(building, pos, Rotation.NONE, this.getOwnerName(), new int[]{this.entity.getId()}, false, false);

        return BuildingServerEvents.getBuildings().size() > existingBuildingCount;
    }

    @Override
    public boolean clearOrder(boolean clearQueue) {
        return false;
    }

    @Override
    public String getName() {
        return TypeIds.toItemName(this.typeId);
    }

    @Override
    public int getHealth() {
        return (int) this.entity.getHealth();
    }

    @Override
    public int getMaxHealth() {
        return (int) this.entity.getMaxHealth();
    }

    @Override
    public boolean isAlive() {
        return this.entity.isAlive();
    }

    @Override
    public boolean isDead() {
        return !this.entity.isAlive();
    }

    @Override
    public float getX() {
        return (float) this.entity.getX();
    }

    @Override
    public float getY() {
        return (float) this.entity.getY();
    }

    @Override
    public float getZ() {
        return (float) this.entity.getZ();
    }

    @Override
    public BlockPos getPos() {
        return this.entity.getOnPos();
    }

    @Override
    public void kill() {
        this.entity.kill();
    }

    @Override
    public int getTypeId() {
        return this.typeId;
    }

    @Override
    public boolean is(int typeId) {
        return this.typeId == typeId;
    }

    @Override
    public int getPopUsed() {
        return this.unit.getCost().population;
    }

    @Override
    public boolean isWorker() {
        return this.worker != null;
    }

    @Override
    public boolean isHero() {
        return this.hero != null;
    }

    @Override
    public int getId() {
        return this.entity.getId();
    }

    @Override
    public boolean isMaxCarryCapacityReached() {
        return Resources.getTotalResourcesFromItems(this.unit.getItems()).getTotalValue() >= this.unit.getMaxResources();
    }

    @Override
    public boolean isCarryCapacityOver(int capacity) {
        return Resources.getTotalResourcesFromItems(this.unit.getItems()).getTotalValue() >= capacity;
    }

    @Override
    public boolean isCarryingItems() {
        return !this.unit.getItems().isEmpty();
    }

    @Override
    public Object getAttackTarget() {
        var targetEntity = this.mob != null
                ? this.mob.getTarget()
                : null;
        if (targetEntity != null) {
            return targetEntity;
        }

        var targetBuilding = this.attacker != null && this.attacker.getAttackBuildingGoal() instanceof MeleeAttackBuildingGoal g
                ? g.getBuildingTarget()
                : null;
        if (targetBuilding != null) {
            return targetBuilding;
        }

        return null;
    }
}
