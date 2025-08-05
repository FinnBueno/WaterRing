package me.finnbueno.waterRing.utils;

import com.projectkorra.projectkorra.ability.CoreAbility;

public class AbilityFinder implements IAbilityFinder {
    @Override
    public CoreAbility getAbilityByName(String name) {
        return CoreAbility.getAbility(name);
    }

    @Override
    public CoreAbility getAbilityByClassName(String className) {
        return CoreAbility.getAbilities().stream()
                .filter(ca -> ca.getClass().getSimpleName().equals(className))
                .findFirst()
                .orElse(null);
    }
}
