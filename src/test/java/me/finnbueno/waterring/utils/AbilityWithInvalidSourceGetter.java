package me.finnbueno.waterring.utils;

import com.projectkorra.projectkorra.Element;
import com.projectkorra.projectkorra.ability.CoreAbility;
import org.bukkit.Location;

public class AbilityWithInvalidSourceGetter extends CoreAbility {

    public String getSourceBlock() { return null; }

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
