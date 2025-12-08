/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models;

import com.daratrix.ronapi.apis.TypeIds;
import com.daratrix.ronapi.apis.WorldApi;
import com.daratrix.ronapi.models.interfaces.*;
import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.resources.ResourceName;
import com.solegendary.reignofnether.resources.ResourceSources;
import com.solegendary.reignofnether.resources.Resources;
import com.solegendary.reignofnether.unit.UnitAction;
import com.solegendary.reignofnether.unit.UnitServerEvents;
import com.solegendary.reignofnether.unit.goals.MeleeAttackBuildingGoal;
import com.solegendary.reignofnether.unit.interfaces.AttackerUnit;
import com.solegendary.reignofnether.unit.interfaces.HeroUnit;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.interfaces.WorkerUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Rotation;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Daratrix
 */
public class ApiHero extends ApiUnit implements IHero {

    public ApiHero(Unit unit) {
        super(unit);
    }

    @Override
    public HeroUnit getHero() {
        return this.hero;
    }

    @Override
    public int getHeroLevel() {
        return this.hero.getHeroLevel();
    }

    @Override
    public int getSkillPoints() {
        return this.hero.getSkillPoints();
    }

    @Override
    public <T extends HeroAbility> boolean upgradeSkill(Class<T> skillAbilityClass, int level) {
        var ability = this.getAbility(skillAbilityClass);
        return ability.rankUp(this.hero);
    }
}
