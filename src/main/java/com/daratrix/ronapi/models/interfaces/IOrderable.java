/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import com.solegendary.reignofnether.ability.Ability;
import com.solegendary.reignofnether.ability.HeroAbility;
import com.solegendary.reignofnether.building.Building;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author Daratrix
 */
public interface IOrderable {
    // queries
    public int getCurrentOrderId();

    public Object getCurrentOrderTarget();
    default public Object getAttackTarget() {
        return null;
    }

    public List<IOrder> getOrderQueue();

    public default boolean isIdle() {
        return this.getCurrentOrderId() == 0;
    }


    public Stream<Ability> getAbilities();

    default public <T extends Ability> T getAbility(Class<T> abilityClass) {
        var abilities = this.getAbilities();
        Ability ability = abilities.filter(a -> a.getClass().isAssignableFrom(abilityClass.getClass())).findFirst().orElse(null);
        if (abilityClass.isInstance(ability)) {
            return abilityClass.cast(ability);
        }

        return null;
    }

    public <T extends Ability> void setAbilityAutocast(Class<T> abilityClass, boolean on);
    public <T extends Ability> boolean isAbilityAutocasting(Class<T> abilityClass);
    public <T extends Ability> boolean isAbilityOffCooldown(Class<T> abilityClass);
    public <T extends HeroAbility> int getAbilityRank(Class<T> abilityClass);

    // mutations
    public default boolean issuePointOrder(int x, int y, int z, int orderId) {
        return this.issuePointOrder(new BlockPos(x, y, z), orderId);
    }

    public boolean issuePointOrder(BlockPos pos, int orderId);

    public boolean issueWidgetOrder(IWidget target, int orderId);

    public boolean issueWidgetOrder(LivingEntity target, int orderId);

    default public boolean issueWidgetOrder(Unit target, int orderId) {
        return this.issueWidgetOrder((LivingEntity) target, orderId);
    }

    public boolean issueWidgetOrder(BuildingPlacement target, int orderId);

    public boolean issueOrder(int typeId);

    public boolean clearOrder(boolean clearQueue);

    //public boolean queuePointOrder(int x, int y, int z, int orderId);
    //public boolean queueWidgetOrder(IWidget target, int orderId);
}
