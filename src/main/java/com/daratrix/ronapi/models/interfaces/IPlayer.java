/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.daratrix.ronapi.models.interfaces;

import com.daratrix.ronapi.models.ApiPlayerBase;
import com.solegendary.reignofnether.resources.ResourceCost;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 *
 * @author Daratrix
 */
public interface IPlayer {
    String getName();

    public int countUnit(String name);
    public int countBuilding(String name);
    public int countResearch(String name);

    public boolean hasResearch(int researchId);

    public int getFood();
    public int getWood();
    public int getOre();

    public int getPopUsed();
    public int getPopCap();

    public int getArmyPop();
    public int getWorkerPop();

    boolean canAfford(int typeId);
    boolean canAfford(ResourceCost cost);

    boolean isDefeated();

    public Stream<IUnit> getUnitsFiltered(Predicate<IUnit> predicate);
    public Stream<IBuilding> getBuildingsFiltered(Predicate<IBuilding> predicate);
    public Stream<ApiPlayerBase> getBasesFiltered(Predicate<ApiPlayerBase> predicate);
}
