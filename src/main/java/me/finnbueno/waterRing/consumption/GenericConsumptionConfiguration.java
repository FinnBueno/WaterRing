package me.finnbueno.waterRing.consumption;

import com.projectkorra.projectkorra.ability.WaterAbility;
import org.bukkit.block.Block;

public class GenericConsumptionConfiguration extends ConsumptionConfiguration {
    public GenericConsumptionConfiguration(int uses) {
        super(uses);
    }

    @Override
    public Block getSourceBlock(WaterAbility ability) {
        // this is where the call to the generically defined getSourceBlock method would go, IF I HAD ONE :(
        // this will be introduced in the next PK release, after which we can make use of it.
        // for now, we use ReflectiveConsumptionConfiguration
        // return ability.getSourceBlock();
        return null;
    }
}
