package me.finnbueno.waterRing;

import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.event.AbilityEndEvent;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WaterRingListener implements Listener {

    @EventHandler
    public void attemptWaterRingConsumption(AbilityStartEvent event) {
        Ability ability = event.getAbility();
        if (ability instanceof WaterAbility waterAbility) {
            WaterRing.attemptAmmunitionConsumption(waterAbility);
        }
    }

    @EventHandler
    public void attemptWaterRingRefund(AbilityEndEvent event) {
        Ability ability = event.getAbility();
        if (ability instanceof WaterAbility waterAbility) {
            WaterRing.attemptAmmunitionRefund(waterAbility);
        }
    }
}
