package me.finnbueno.waterring.utils;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.WaterAbility;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class AbilityWithValidSourceGetter extends WaterAbility {

    public AbilityWithValidSourceGetter() {
        super(null);
    }

    public Block getSourceBlock() {
        return null;
    }

    @Override
    public void progress() {

    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isIgniteAbility() {
        return false;
    }

    @Override
    public boolean isExplosiveAbility() {
        return false;
    }

    @Override
    public long getCooldown() {
        return 0;
    }

    @Override
    public String getName() {
        return "AbilityName";
    }

    @Override
    public Element getElement() {
        return null;
    }

    @Override
    public Location getLocation() {
        return null;
    }
}
