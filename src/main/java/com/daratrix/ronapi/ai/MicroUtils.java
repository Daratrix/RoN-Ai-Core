package com.daratrix.ronapi.ai;

import com.daratrix.ronapi.apis.PlayerApi;
import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.ApiUnit;
import com.daratrix.ronapi.models.interfaces.IBoxed;
import com.daratrix.ronapi.models.interfaces.IOrderable;
import com.daratrix.ronapi.models.interfaces.IPlayerWidget;
import com.daratrix.ronapi.models.interfaces.IUnit;
import com.daratrix.ronapi.utils.GeometryUtils;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.abilities.*;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.BloodMoon;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.InsomniaCurse;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.RaiseDead;
import com.solegendary.reignofnether.ability.heroAbilities.necromancer.SoulSiphonPassive;
import com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant.FancyFeast;
import com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant.GreedIsGoodPassive;
import com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant.LootExplosion;
import com.solegendary.reignofnether.ability.heroAbilities.piglinmerchant.ThrowTNT;
import com.solegendary.reignofnether.ability.heroAbilities.royalguard.Avatar;
import com.solegendary.reignofnether.ability.heroAbilities.royalguard.MaceSlam;
import com.solegendary.reignofnether.ability.heroAbilities.royalguard.TauntingCry;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.production.ProductionItems;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.research.ResearchServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.interfaces.RangedAttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.SpiderUnit;
import com.solegendary.reignofnether.unit.units.monsters.WardenUnit;
import com.solegendary.reignofnether.unit.units.piglins.BruteUnit;
import com.solegendary.reignofnether.unit.units.piglins.WitherSkeletonUnit;
import com.solegendary.reignofnether.unit.units.villagers.EvokerUnit;
import com.solegendary.reignofnether.unit.units.villagers.RavagerUnit;
import com.solegendary.reignofnether.unit.units.villagers.WitchUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Consumer;

public class MicroUtils {

    public static HashMap<Integer, Consumer<IPlayerWidget>> defaultMicroLogics = new HashMap<>();

    static {
        defaultMicroLogics.put(TypeIds.Villagers.Ravager, MicroUtils::microRavager);
        defaultMicroLogics.put(TypeIds.Villagers.HeroRoyalGuard, MicroUtils::microRoyalGuard);

        defaultMicroLogics.put(TypeIds.Monsters.Warden, MicroUtils::microWarden);
        defaultMicroLogics.put(TypeIds.Monsters.HeroNecromancer, MicroUtils::microNecromancer);

        defaultMicroLogics.put(TypeIds.Piglins.Brute, MicroUtils::microBrute);
        defaultMicroLogics.put(TypeIds.Piglins.Headhunter, MicroUtils::microHeadhunter);
        defaultMicroLogics.put(TypeIds.Piglins.WitherSkeleton, MicroUtils::microWitherSkeleton);
        defaultMicroLogics.put(TypeIds.Piglins.HeroMerchant, MicroUtils::microMerchant);
    }

    public static Consumer<IPlayerWidget> getMicroLogic(int typeId) {
        return defaultMicroLogics.getOrDefault(typeId, null);
    }

    public static void enableAutocastAbilities(IPlayerWidget u) {
        if (u.is(TypeIds.Villagers.Witch)) {
            u.setAbilityAutocast(ThrowLingeringRegenPotion.class, true);
            u.setAbilityAutocast(ThrowWaterPotion.class, true);
        }

        if (u.is(TypeIds.Villagers.Evoker)) {
            u.setAbilityAutocast(CastSummonVexes.class, true);
        }

        if (u.isAnyOf(TypeIds.Monsters.Spider, TypeIds.Monsters.PoisonSpider)) {
            u.setAbilityAutocast(SpinWebs.class, true);
        }
    }

    public static void microRavager(IPlayerWidget unit) {
        if (!unit.isAbilityOffCooldown(Roar.class)) {
            return;
        }

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()) && GeometryUtils.isWithinDistance(unit, e, 4))) {
            unit.issueOrder(TypeIds.Orders.Roar);
            return;
        }
    }

    public static void microRoyalGuard(IPlayerWidget unit) {
        var target = unit.getAttackTarget();
        LivingEntity entityTarget = (target instanceof LivingEntity e) && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()))
                ? e
                : null;
        if (entityTarget == null) {
            return;
        }

        if (unit.getAbilityRank(MaceSlam.class) > 0 && unit.isAbilityOffCooldown(MaceSlam.class) && GeometryUtils.isWithinDistance(unit, entityTarget, 4)) {
            unit.issueWidgetOrder(entityTarget, TypeIds.Orders.MaceSlam);
            return;
        }

        boolean hasLowHealth = unit.getHealth() < unit.getMaxHealth() * 0.5; // <50%
        if (unit.getAbilityRank(TauntingCry.class) > 0 && !hasLowHealth && unit.isAbilityOffCooldown(TauntingCry.class) && GeometryUtils.isWithinDistance(unit, entityTarget, 6)) {
            unit.issueOrder(TypeIds.Orders.Taunt);
            return;
        }

        if (unit.getAbilityRank(Avatar.class) > 0 && unit.isAbilityOffCooldown(Avatar.class) && GeometryUtils.isWithinDistance(unit, entityTarget, 8)) {
            unit.issueOrder(TypeIds.Orders.Avatar);
            return;
        }
    }

    public static void microWarden(IPlayerWidget unit) {
        if (!unit.isAbilityOffCooldown(SonicBoom.class)) {
            return;
        }

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && e.getHealth() > 30 && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()))) {
            unit.issueWidgetOrder(e, TypeIds.Orders.SonicBoom);
            return;
        }

        if (target instanceof BuildingPlacement b && !b.ownerName.equals(unit.getOwnerName())) {
            unit.issueWidgetOrder(b, TypeIds.Orders.SonicBoom);
            return;
        }
    }

    public static void microNecromancer(IPlayerWidget unit) {
        var target = unit.getAttackTarget();
        LivingEntity entityTarget = (target instanceof LivingEntity e) && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()))
                ? e
                : null;

        if (unit.getAbilityRank(RaiseDead.class) > 0 && entityTarget != null && unit.isAbilityOffCooldown(RaiseDead.class) && GeometryUtils.isWithinDistance(unit, entityTarget, 10)) {
            unit.setAbilityAutocast(SoulSiphonPassive.class, true); // always boost Raise Dead
            unit.issueOrder(TypeIds.Orders.RaiseDead);
            return;
        }

        if (unit.getAbilityRank(InsomniaCurse.class) > 0 && entityTarget != null && unit.isAbilityOffCooldown(InsomniaCurse.class) && GeometryUtils.isWithinDistance(unit, entityTarget, 12)) {
            unit.setAbilityAutocast(SoulSiphonPassive.class, true); // always boost Raise Dead
            unit.issueWidgetOrder(entityTarget, TypeIds.Orders.InsomniaCurse);
            return;
        }

        // just spam the ultimate kekw
        if (unit.getAbilityRank(BloodMoon.class) > 0 && unit.isAbilityOffCooldown(BloodMoon.class)) {
            unit.setAbilityAutocast(SoulSiphonPassive.class, true); // always boost Raise Dead
            unit.issueOrder(TypeIds.Orders.BloodMoon);
            return;
        }
    }

    public static void microBrute(IPlayerWidget unit) {
        var canShield = ResearchServerEvents.playerHasResearch(unit.getOwnerName(), ProductionItems.RESEARCH_BRUTE_SHIELDS);
        var canBloodlust = ResearchServerEvents.playerHasResearch(unit.getOwnerName(), ProductionItems.RESEARCH_BLOODLUST)
                && unit.getHealth() > unit.getMaxHealth() / 2; // only cast bloodlust when over 50% health

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName())) && GeometryUtils.isWithinDistance(unit, e, 10)) {
            if (canBloodlust && unit.isAbilityOffCooldown(Bloodlust.class)) unit.issueOrder(TypeIds.Orders.BloodLust);
            if (target instanceof RangedAttackerUnit) {
                if (canShield && !unit.isAbilityAutocasting(ToggleShield.class)) unit.issueOrder(TypeIds.Orders.ShieldOn);
            }
            return;
        }

        if (target instanceof BuildingPlacement b && !b.ownerName.equals(unit.getOwnerName())) {
            if (canBloodlust && unit.isAbilityOffCooldown(Bloodlust.class)) unit.issueOrder(TypeIds.Orders.BloodLust);
            if (canShield && !unit.isAbilityAutocasting(ToggleShield.class)) unit.issueOrder(TypeIds.Orders.ShieldOn);
            return;
        }

        if (!canShield && unit.isAbilityAutocasting(ToggleShield.class)) unit.issueOrder(TypeIds.Orders.ShieldOff);
    }

    public static void microHeadhunter(IPlayerWidget unit) {
        var canBloodlust = ResearchServerEvents.playerHasResearch(unit.getOwnerName(), ProductionItems.RESEARCH_BLOODLUST)
                && unit.getHealth() > unit.getMaxHealth() / 2; // only cast bloodlust when over 50% health

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName())) && GeometryUtils.isWithinDistance(unit, e, 14)) {
            if (canBloodlust && unit.isAbilityOffCooldown(Bloodlust.class)) unit.issueOrder(TypeIds.Orders.BloodLust);
            return;
        }
    }

    public static void microWitherSkeleton(IPlayerWidget unit) {
        if (!unit.isAbilityOffCooldown(WitherCloud.class)) {
            return;
        }

        var target = unit.getAttackTarget();
        if (target instanceof LivingEntity e && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName())) && GeometryUtils.isWithinDistance(unit, e, 4)) {
            unit.issueOrder(TypeIds.Orders.WitherCloud);
            return;
        }
    }

    public static void microMerchant(IPlayerWidget unit) {
        var target = unit.getAttackTarget();
        LivingEntity entityTarget = (target instanceof LivingEntity e) && (!(target instanceof Unit u) || !u.getOwnerName().equals(unit.getOwnerName()))
                ? e
                : null;

        // control greedisgood before casting any other ability
        var player = WorldApi.getSingleton().players.get(unit.getOwnerName());
        boolean highFood = player.getFood() > 500;
        boolean highWood = player.getWood() > 500;
        boolean highOre = player.getOre() > 500;

        if (unit.getAbilityRank(ThrowTNT.class) > 0 && unit.isAbilityOffCooldown(ThrowTNT.class) && entityTarget != null && GeometryUtils.isWithinDistance(unit, entityTarget, 20)) {
            unit.setAbilityAutocast(GreedIsGoodPassive.class, highWood);
            unit.issueWidgetOrder(entityTarget, TypeIds.Orders.ThrowTnt);
            return;
        }

        boolean hasLowHealth = unit.getHealth() < unit.getMaxHealth() * 0.5; // <50%
        if (unit.getAbilityRank(FancyFeast.class) > 0 && unit.isAbilityOffCooldown(FancyFeast.class) && hasLowHealth) {
            unit.setAbilityAutocast(GreedIsGoodPassive.class, highFood);
            unit.issuePointOrder(unit.getPos(), TypeIds.Orders.FancyFeast);
            return;
        }

        // just spam the ultimate kekw
        boolean hasHighMana = unit.getMana() > unit.getMaxMana() * 0.5; // >50%
        if (unit.getAbilityRank(LootExplosion.class) > 0 && unit.isAbilityOffCooldown(LootExplosion.class) && hasHighMana) {
            unit.setAbilityAutocast(GreedIsGoodPassive.class, highOre);
            unit.issueOrder(TypeIds.Orders.LootExplosion);
            return;
        }
    }
}
