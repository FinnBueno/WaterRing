package me.finnbueno.waterRing;

import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.event.AbilityEndEvent;
import com.projectkorra.projectkorra.event.AbilityStartEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WaterRingListener implements Listener {

    @EventHandler
    public void attemptWaterRingConsumption(AbilityStartEvent event) {
        Ability ability = event.getAbility();
        if (ability instanceof WaterAbility waterAbility) {
            if (!WaterRing.isSourcedFromWaterRing(waterAbility)) {
                return;
            }
            boolean wasSuccessfullyConsumed = WaterRing.attemptAmmunitionConsumption(waterAbility);
            if (!wasSuccessfullyConsumed) {
                event.setCancelled(true);
                Player player = ability.getPlayer();
                player.playSound(player.getEyeLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.4F, 0.4F);
                TextComponent textComponent = new TextComponent("Not enough water!");
                textComponent.setColor(ChatColor.BLUE);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, textComponent);
            }
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
