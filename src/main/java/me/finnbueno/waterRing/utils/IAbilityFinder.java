package me.finnbueno.waterRing.utils;

import com.projectkorra.projectkorra.ability.CoreAbility;

public interface IAbilityFinder {

    public CoreAbility getAbilityByName(String name);
    public CoreAbility getAbilityByClassName(String className);
}
