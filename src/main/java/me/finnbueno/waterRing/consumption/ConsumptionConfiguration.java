package me.finnbueno.waterRing.consumption;

import com.projectkorra.projectkorra.ability.WaterAbility;
import org.bukkit.block.Block;

public abstract class ConsumptionConfiguration {

    private final String abilityName;
    private final int uses;
    private final boolean refundable;

    public ConsumptionConfiguration(String abilityName, int uses) {
        this(abilityName, uses, false);
    }

    public ConsumptionConfiguration(String abilityName, int uses, boolean refundable) {
        this.abilityName = abilityName;
        this.uses = uses;
        this.refundable = refundable;
    }

    public abstract Block getSourceBlock(WaterAbility ability);

    public int getUses() {
        return uses;
    }

    public boolean isRefundable() {
        return refundable;
    }

    public String getAbilityName() {
        return abilityName;
    }
}
