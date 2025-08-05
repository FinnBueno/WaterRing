package me.finnbueno.waterRing.consumption;

import com.projectkorra.projectkorra.ability.WaterAbility;
import org.bukkit.block.Block;

public abstract class ConsumptionConfiguration {

    private final int uses;
    private final boolean refundable;

    public ConsumptionConfiguration(int uses) {
        this(uses, false);
    }

    public ConsumptionConfiguration(int uses, boolean refundable) {
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
}
