package me.finnbueno.waterRing.consumption;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.WaterAbility;
import org.bukkit.block.Block;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

public class ReflectiveConsumptionConfiguration extends ConsumptionConfiguration {

    private final Method getSourceBlockMethod;

    public ReflectiveConsumptionConfiguration(String abilityName, Method getSourceBlockMethod, int uses, boolean isRefundable) {
        super(abilityName, uses, isRefundable);
        this.getSourceBlockMethod = getSourceBlockMethod;
    }

    @Override
    public Block getSourceBlock(WaterAbility ability) {
        try {
            return (Block) getSourceBlockMethod.invoke(ability);
        } catch (IllegalAccessException | InvocationTargetException e) {
            ProjectKorra.plugin.getLogger().log(
                    Level.SEVERE,
                    "Encountered error while retrieving source block for %s. Consider removing it from the WaterRing entry list".formatted(
                            ability.getName()),
                    e);
        }
        return null;
    }
}
