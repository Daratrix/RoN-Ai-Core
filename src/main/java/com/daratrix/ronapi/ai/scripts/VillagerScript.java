package com.daratrix.ronapi.ai.scripts;

import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.models.interfaces.IHero;
import com.solegendary.reignofnether.ability.heroAbilities.royalguard.Avatar;
import com.solegendary.reignofnether.ability.heroAbilities.royalguard.BattleRagePassive;
import com.solegendary.reignofnether.ability.heroAbilities.royalguard.MaceSlam;
import com.solegendary.reignofnether.ability.heroAbilities.royalguard.TauntingCry;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.world.level.Level;

public class VillagerScript extends IAiLogic.AbstractAiLogic {

    public static void register() {
        AiLogics.registerAiLogic(new VillagerScript());
    }

    public static final String name = "VillagerScript";

    public VillagerScript() {
        super();

        this.heroSkillLogics.put(TypeIds.Villagers.HeroRoyalGuard, VillagerScript::RoyalGuardSkills);
    }

    private static void RoyalGuardSkills(IHero hero) {
        hero.upgradeSkill(MaceSlam.class, 1);
        hero.upgradeSkill(BattleRagePassive.class, 1);
        hero.upgradeSkill(TauntingCry.class, 1);
        hero.upgradeSkill(MaceSlam.class, 2);
        hero.upgradeSkill(BattleRagePassive.class, 2);
        hero.upgradeSkill(Avatar.class, 1);
        hero.upgradeSkill(TauntingCry.class, 2);
        hero.upgradeSkill(MaceSlam.class, 3);
        hero.upgradeSkill(BattleRagePassive.class, 3);
        hero.upgradeSkill(TauntingCry.class, 3);
    }

    private static final int maxFarms = 15;
    private static final int maxWood = 15;
    private static final int maxOre = 6;
    private static final int maxBuffer = 3; // make just a few more workers for buildings  and extra wood
    private static final int targetWorkers = maxFarms + maxWood + maxOre + maxBuffer;

    private void setHarvestPriorities(IAiPlayer player, AiHarvestPriorities harvestingPriorities) {

        var farmCount = player.countDone(this.getFarmTypeId());
        var villagerCount = player.countDone(this.getWorkerTypeId());
        var oreReduction = player.getOre() / 300;

        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, 1); // at least one on wood

        if (villagerCount < 15) {
            harvestingPriorities.addPriority(TypeIds.Resources.FoodEntity, 2 - player.getFood() / 200 - farmCount / 2);
            harvestingPriorities.addPriority(TypeIds.Resources.FoodBlock, 4 - player.getFood() / 100 - farmCount);
            harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, villagerCount - farmCount); // at least one wood per farm
            harvestingPriorities.addPriority(TypeIds.Resources.FoodFarm, farmCount); // each farm need a farmer
            return;
        }

        var oreWorkers = Math.min(farmCount / 3 - oreReduction, maxOre); // for every 3 farms, add an ore worker
        harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, Math.min(oreWorkers, maxOre / 2));
        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, farmCount / 2); // at least one wood per 2 farm
        harvestingPriorities.addPriority(TypeIds.Resources.FoodFarm, farmCount); // each farm need a worker
        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, farmCount);
        harvestingPriorities.addPriority(TypeIds.Resources.OreBlock, Math.min(oreWorkers, maxOre));

        // in case of excessive workers, assign to wood
        harvestingPriorities.addPriority(TypeIds.Resources.WoodBlock, maxWood + maxBuffer);
    }

    public void setBuildingPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        var farmCount = player.countDone(this.getFarmTypeId());
        var villagerCount = player.countDone(this.getWorkerTypeId());

        priorities.setDefaultLocation(AiProductionPriorities.Location.MAIN);

        // until the Town Centre is done, it should be the only priority
        if (player.countDone(TypeIds.Villagers.TownCentre) == 0) {
            priorities.addPriority(TypeIds.Villagers.TownCentre, 1);
            return;
        }

        if (player.count(TypeIds.Villagers.Villager) < 6) {
            return; // wait to have a few villagers before making the first house/building
        }

        // dynamically add houses any time we have less than 5 pop space to spare
        int productionPower = 4 // TC buffer
                + Math.min(farmCount / 3, player.count(TypeIds.Villagers.Barracks)) * 3
                + player.count(TypeIds.Villagers.Blacksmith) * 4
                + player.count(TypeIds.Villagers.ArcaneTower) * 3
                + player.count(TypeIds.Villagers.Castle) * 6;
        var popCap = player.getPopCap();
        var popUsed = player.getPopUsed();
        var popFree = popCap - popUsed;
        priorities.logger.log("productionPower: " + productionPower + "(" + popUsed + "/" + popCap + ")");
        if (popCap < this.getMaxPopulation() && popFree < productionPower) {
            var existing = player.countDone(TypeIds.Villagers.House);
            var target = 1 + (int) (productionPower / 10) + existing;
            priorities.logger.log("more houses: " + existing + "=>" + target);
            priorities.addPriority(TypeIds.Villagers.House, target).atSide();
            if (popCap - popUsed < 3) {
                return;
            }
        }

        // dynamically create stockpiles every time nearest resource distance becomes too high
        //var townCentre = player.getUnitsFiltered(u -> u.getTypeId() == TypeIds.Villagers.TownCentre);
        //var stockpiles = player.getUnitsFiltered(u -> u.getTypeId() == TypeIds.Villagers.Stockpile);
        //var dropoffPoints = Stream.concat(townCentre, stockpiles);
        //if (dropoffPoints.noneMatch(u -> WorldApi.hasResourcesWithinDistanceOfType(u, 32, TypeIds.Resources.WoodBlock))) {
        //
        //}
        //if (dropoffPoints.noneMatch(u -> WorldApi.hasResourcesWithinDistanceOfType(u, 32, TypeIds.Resources.OreBlock))) {
        //
        //}

        //priorities.addPriority(TypeIds.Villagers.Stockpile, 1);
        priorities.addPriority(TypeIds.Villagers.House, 1).atSide(); // at least one house before farm
        priorities.addPriority(TypeIds.Villagers.Farm, 1).atFarm();

        priorities.addPriority(TypeIds.Villagers.Barracks, 1).atFront();
        priorities.addPriority(TypeIds.Villagers.Farm, 3).atFarm();
        priorities.addPriority(TypeIds.Villagers.Barracks, 2).atFront().limit(farmCount / 3);
        priorities.addPriority(TypeIds.Villagers.Farm, 6).atFarm();
        priorities.addPriority(TypeIds.Villagers.Altar, 1).atBack().required();
        priorities.addPriority(TypeIds.Villagers.Blacksmith, 1).atBack();
        priorities.addPriority(TypeIds.Villagers.Farm, 8).atFarm();
        priorities.addPriority(TypeIds.Villagers.ArcaneTower, 1).atBack().required();
        priorities.addPriority(TypeIds.Villagers.Farm, 10).atFarm();
        priorities.addPriority(TypeIds.Villagers.Barracks, 3).atFront().limit(farmCount / 3);
        priorities.addPriority(TypeIds.Villagers.Library, 1).atBack();
        priorities.addPriority(TypeIds.Villagers.Barracks, 4).atFront();
        priorities.addPriority(TypeIds.Villagers.GrandLibrary, 1);
        priorities.addPriority(TypeIds.Villagers.Castle, 1).atFront().required();
        priorities.addPriority(TypeIds.Villagers.Barracks, 5).atFront().limit(farmCount / 3);
        priorities.addPriority(TypeIds.Villagers.OfficersQuarters, 1);
        priorities.addPriority(TypeIds.Villagers.Farm, maxFarms).atFarm();
    }

    public void setUnitPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Villagers.Villager, 8);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 2);
        priorities.addPriority(TypeIds.Villagers.Pillager, 2);
        priorities.addPriority(TypeIds.Villagers.Villager, 16);
        priorities.addPriority(TypeIds.Villagers.HeroRoyalGuard, 1);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 6);
        priorities.addPriority(TypeIds.Villagers.Pillager, 6);
        priorities.addPriority(TypeIds.Villagers.Villager, 24);
        priorities.addPriority(TypeIds.Villagers.Witch, 3);
        priorities.addPriority(TypeIds.Villagers.Vindicator, 12);
        priorities.addPriority(TypeIds.Villagers.Pillager, 9);
        priorities.addPriority(TypeIds.Villagers.Villager, targetWorkers).required();
        priorities.addPriority(TypeIds.Villagers.IronGolem, 3);
        priorities.addPriority(TypeIds.Villagers.Evoker, 3);
        priorities.addPriority(TypeIds.Villagers.Ravager, 3);
    }

    public void setResearchPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Villagers.LingeringPotions, 1);
        priorities.addPriority(TypeIds.Villagers.HealingPotions, 1);
        priorities.addPriority(TypeIds.Villagers.WaterPotions, 1);
        priorities.addPriority(TypeIds.Villagers.Vexes, 1);
        priorities.addPriority(TypeIds.Villagers.RavagerCavalry, 1);
    }

    private void setArmyPriorities(Level level, IAiPlayer player, AiArmyPriorities armyPriorities) {
        armyPriorities.pickDefaultGatherPoint(level, player);
        if (armyPriorities.pickDefenseTarget(player)) {
            armyPriorities.attackTarget = null; // don't attack while bases are threatened
        } else {
            armyPriorities.pickAttackTarget(player);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Faction getFaction() {
        return Faction.VILLAGERS;
    }

    @Override
    public void setPriorities(Level level, IAiPlayer player, IAiControllerPriorities priorities) {
        this.setHarvestPriorities(player, priorities.getHarvestingPriorities());
        this.setBuildingPriorities(player, priorities.getBuildingPriorities());
        this.setUnitPriorities(player, priorities.getUnitPriorities());
        this.setResearchPriorities(player, priorities.getResearchPriorities());
        this.setArmyPriorities(level, player, priorities.getArmyPriorities());
    }
}
