package me.finnbueno.waterRing;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.util.ClickType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

public class WaterRing extends WaterAbility implements AddonAbility, ComboAbility {

    public WaterRing(Player player) {
        super(player);
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
    public long getCooldown() {
        return 0;
    }

    @Override
    public String getName() {
        return "WaterRing";
    }

    @Override
    public Location getLocation() {
        return player.getEyeLocation();
    }

    @Override
    public void load() {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getAuthor() {
        return "FinnBueno";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public WaterRing createNewComboInstance(Player player) {
        return new WaterRing(player);
    }

    @Override
    public ArrayList<ComboManager.AbilityInformation> getCombination() {
        return new ArrayList<>(Arrays.asList(
                new ComboManager.AbilityInformation("Torrent", ClickType.SHIFT_DOWN),
                new ComboManager.AbilityInformation("WaterManipulation", ClickType.LEFT_CLICK),
                new ComboManager.AbilityInformation("WaterManipulation", ClickType.LEFT_CLICK),
                new ComboManager.AbilityInformation("Torrent", ClickType.SHIFT_UP)
        ));
    }
}
