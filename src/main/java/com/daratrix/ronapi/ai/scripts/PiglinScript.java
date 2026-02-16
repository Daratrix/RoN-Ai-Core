package com.daratrix.ronapi.ai.scripts;

import com.daratrix.ronapi.ai.controller.AiLogics;
import com.daratrix.ronapi.ai.controller.interfaces.IAiControllerPriorities;
import com.daratrix.ronapi.ai.controller.interfaces.IAiLogic;
import com.daratrix.ronapi.ai.player.interfaces.IAiPlayer;
import com.daratrix.ronapi.ai.priorities.AiArmyPriorities;
import com.daratrix.ronapi.ai.priorities.AiHarvestPriorities;
import com.daratrix.ronapi.ai.priorities.AiProductionPriorities;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.models.interfaces.IHero;
import com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant.FancyFeast;
import com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant.GreedIsGoodPassive;
import com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant.LootExplosion;
import com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant.ThrowTNT;
import com.solegendary.reignofnether.faction.Faction;
import net.minecraft.world.level.Level;

public class PiglinScript extends IAiLogic.AbstractAiLogic {

    public static void register() {
        AiLogics.registerAiLogic(new PiglinScript());
    }

    public static final String name = "PiglinScript";

    public PiglinScript() {
        super();

        this.heroSkillLogics.put(TypeIds.Piglins.HeroMerchant, PiglinScript::MerchantSkills);
    }

    private static void MerchantSkills(IHero hero) {
        hero.upgradeSkill(ThrowTNT.class, 1);
        hero.upgradeSkill(FancyFeast.class, 1);
        hero.upgradeSkill(GreedIsGoodPassive.class, 1);
        hero.upgradeSkill(ThrowTNT.class, 2);
        hero.upgradeSkill(FancyFeast.class, 2);
        hero.upgradeSkill(LootExplosion.class, 1);
        hero.upgradeSkill(GreedIsGoodPassive.class, 2);
        hero.upgradeSkill(ThrowTNT.class, 3);
        hero.upgradeSkill(FancyFeast.class, 3);
        hero.upgradeSkill(GreedIsGoodPassive.class, 3);
    }

    protected int portalCount = 0;
    protected int civilianPortalCount = 0;
    protected int militaryPortalCount = 0;
    protected int advancedPortalCount = 0;

    protected AiProductionPriorities.AiProductionPriority BuildPortal(AiProductionPriorities priorities, int count) {
        this.portalCount = count;
        return priorities.addPriority(TypeIds.Piglins.Portal, count);
    }

    protected AiProductionPriorities.AiProductionPriority BuildCivilianPortal(AiProductionPriorities priorities, int count) {
        this.BuildPortal(priorities, count + militaryPortalCount + advancedPortalCount).atSide().builders(2); // for supply, we need faster construction
        this.civilianPortalCount = count;
        return priorities.addPriority(TypeIds.Piglins.CivilianPortal, count).atSide();
    }

    protected AiProductionPriorities.AiProductionPriority BuildMilitaryPortal(AiProductionPriorities priorities, int count, int limit) {
        this.BuildPortal(priorities, civilianPortalCount + count + advancedPortalCount).atFront();
        this.militaryPortalCount = count;
        return priorities.addPriority(TypeIds.Piglins.MilitaryPortal, count).atFront().limit(limit);
    }

    protected AiProductionPriorities.AiProductionPriority BuildAdvancedPortal(AiProductionPriorities priorities, int count, AiProductionPriorities.Location location) {
        this.BuildPortal(priorities, civilianPortalCount + militaryPortalCount + count).atLocation(location);
        this.advancedPortalCount = count;
        return priorities.addPriority(TypeIds.Piglins.AdvancedPortals, count).atLocation(location);
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
        if (player.countDone(TypeIds.Piglins.MainPortal) == 0) {
            priorities.addPriority(TypeIds.Piglins.MainPortal, 1);
            return;
        }

        // dynamically add houses any time we have less than 5 pop space to spare
        int productionPower = 4 // TC buffer
                + player.count(TypeIds.Piglins.MilitaryPortal) * 3;
        var popCap = player.getPopCap();
        var popUsed = player.getPopUsed();
        var popFree = popCap - popUsed;
        priorities.logger.log("productionPower: " + productionPower + "(" + popUsed + "/" + popCap + ")");
        if (popCap < this.getMaxPopulation() && popFree < productionPower) {
            var existing = player.countDone(TypeIds.Piglins.CivilianPortal);
            var target = 1 + (int) (productionPower / 15) + existing;
            priorities.logger.log("more houses: " + existing + "=>" + target);
            this.BuildCivilianPortal(priorities, target);
            //if (popCap - popUsed < 3) {
            //    return;
            //}
        }

        // dynamically create stockpiles every time nearest resource distance becomes too high
        //var townCentre = player.getUnitsFiltered(u -> u.getTypeId() == TypeIds.Piglins.TownCentre);
        //var stockpiles = player.getUnitsFiltered(u -> u.getTypeId() == TypeIds.Piglins.Stockpile);
        //var dropoffPoints = Stream.concat(townCentre, stockpiles);
        //if (dropoffPoints.noneMatch(u -> WorldApi.hasResourcesWithinDistanceOfType(u, 32, TypeIds.Resources.WoodBlock))) {
        //
        //}
        //if (dropoffPoints.noneMatch(u -> WorldApi.hasResourcesWithinDistanceOfType(u, 32, TypeIds.Resources.OreBlock))) {
        //
        //}

        //priorities.addPriority(TypeIds.Piglins.Stockpile, 1);
        this.BuildCivilianPortal(priorities, 1); // at least one house before farm
        priorities.addPriority(TypeIds.Piglins.Farm, 1).atFarm();
        this.BuildMilitaryPortal(priorities, 1, 1);
        priorities.addPriority(TypeIds.Piglins.Farm, 4).atFarm();
        this.BuildMilitaryPortal(priorities, 2, farmCount / 2);
        priorities.addPriority(TypeIds.Piglins.Farm, 6).atFarm();
        priorities.addPriority(TypeIds.Piglins.Altar, 1).atBack().required();
        priorities.addPriority(TypeIds.Piglins.Bastion, 1).atFront().required().builders(2);
        this.BuildMilitaryPortal(priorities, 4, farmCount / 2);
        priorities.addPriority(TypeIds.Piglins.HoglinStables, 1).atBack();
        priorities.addPriority(TypeIds.Piglins.FlameSanctuary, 1).atBack();
        priorities.addPriority(TypeIds.Piglins.WitherShrine, 1).atBack();
        priorities.addPriority(TypeIds.Piglins.Farm, 10).atFarm();
        priorities.addPriority(TypeIds.Piglins.Fortress, 1).atFront().required().builders(5);
        priorities.addPriority(TypeIds.Piglins.Farm, maxFarms).atFarm();
        this.BuildMilitaryPortal(priorities, 7, farmCount / 2);
    }

    public void setUnitPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Piglins.Grunt, 8);
        priorities.addPriority(TypeIds.Piglins.Brute, 3);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 4);
        priorities.addPriority(TypeIds.Piglins.Grunt, 16);
        priorities.addPriority(TypeIds.Piglins.Brute, 10);
        priorities.addPriority(TypeIds.Piglins.HeroMerchant, 1);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 12);
        priorities.addPriority(TypeIds.Piglins.Grunt, targetWorkers).required();
        priorities.addPriority(TypeIds.Piglins.Brute, 14);
        priorities.addPriority(TypeIds.Piglins.Headhunter, 14);
        priorities.addPriority(TypeIds.Piglins.Blaze, 5);
        priorities.addPriority(TypeIds.Piglins.WitherSkeleton, 6);
        priorities.addPriority(TypeIds.Piglins.Ghast, 3);
    }

    public void setResearchPriorities(IAiPlayer player, AiProductionPriorities priorities) {
        priorities.addPriority(TypeIds.Piglins.HeadhunterTrident, 1);
        priorities.addPriority(TypeIds.Piglins.BruteShield, 1);
        priorities.addPriority(TypeIds.Piglins.HoglinCavalry, 1);
        priorities.addPriority(TypeIds.Piglins.FireResistance, 1);
        priorities.addPriority(TypeIds.Piglins.BlazeFirewall, 1);
        priorities.addPriority(TypeIds.Piglins.WitherCloud, 1);
        priorities.addPriority(TypeIds.Piglins.AdvancedPortals, 1);
        priorities.addPriority(TypeIds.Piglins.SoulFireballs, 1);
        priorities.addPriority(TypeIds.Piglins.Bloodlust, 1);
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
        return Faction.PIGLINS;
    }

    @Override
    public void setPriorities(Level level, IAiPlayer player, IAiControllerPriorities priorities) {
        this.portalCount = 0;
        this.civilianPortalCount = 0;
        this.militaryPortalCount = 0;
        this.advancedPortalCount = 0;
        this.setHarvestPriorities(player, priorities.getHarvestingPriorities());
        this.setBuildingPriorities(player, priorities.getBuildingPriorities());
        this.setUnitPriorities(player, priorities.getUnitPriorities());
        this.setResearchPriorities(player, priorities.getResearchPriorities());
        this.setArmyPriorities(level, player, priorities.getArmyPriorities());
    }
}
