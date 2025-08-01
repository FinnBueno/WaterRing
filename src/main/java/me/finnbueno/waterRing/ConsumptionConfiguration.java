package me.finnbueno.waterRing;

import com.projectkorra.projectkorra.ability.Ability;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.waterbending.SurgeWall;
import com.projectkorra.projectkorra.waterbending.SurgeWave;
import com.projectkorra.projectkorra.waterbending.Torrent;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;
import org.bukkit.block.Block;

public abstract class ConsumptionConfiguration<T extends WaterAbility> {

    public static final ConsumptionConfiguration<Torrent> TORRENT = new TorrentConsumption();
    public static final ConsumptionConfiguration<WaterManipulation> WATER_MANIPULATION = new WaterManipulationConsumption();
    public static final ConsumptionConfiguration<SurgeWall> SURGE_WALL = new SurgeWallConsumption();
    public static final ConsumptionConfiguration<SurgeWave> SURGE_WAVE = new SurgeWaveConsumption();

    private final int uses;
    private final boolean refundable;

    public ConsumptionConfiguration(int uses) {
        this(uses, false);
    }

    public ConsumptionConfiguration(int uses, boolean refundable) {
        this.uses = uses;
        this.refundable = refundable;
    }

    public abstract Block getSourceBlock(T ability);

    public int getUses() {
        return uses;
    }

    public boolean isRefundable() {
        return refundable;
    }

    private static class TorrentConsumption extends ConsumptionConfiguration<Torrent> {
        public TorrentConsumption() {
            super(-1);
        }

        @Override
        public Block getSourceBlock(Torrent ability) {
            return ability.getSourceBlock();
        }
    }

    private static class WaterManipulationConsumption extends ConsumptionConfiguration<WaterManipulation> {
        public WaterManipulationConsumption() {
            super(1);
        }

        @Override
        public Block getSourceBlock(WaterManipulation ability) {
            return ability.getSourceBlock();
        }
    }

    private static class SurgeWallConsumption extends ConsumptionConfiguration<SurgeWall> {
        public SurgeWallConsumption() {
            super(5, true);
        }

        @Override
        public Block getSourceBlock(SurgeWall ability) {
            return ability.getSourceBlock();
        }
    }

    private static class SurgeWaveConsumption extends ConsumptionConfiguration<SurgeWave> {
        public SurgeWaveConsumption() {
            super(-1);
        }

        @Override
        public Block getSourceBlock(SurgeWave ability) {
            return ability.getSourceBlock();
        }
    }
}
