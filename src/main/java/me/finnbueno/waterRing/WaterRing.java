package me.finnbueno.waterRing;

import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.BlockSourceInformation;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class WaterRing extends WaterAbility implements AddonAbility, ComboAbility {

    /*
    Per player, per type of source (water, earth, etc.), per clicking type (shift or click), track a block
     */
    private static Map<Player, Map<BlockSource.BlockSourceType, Map<ClickType, BlockSourceInformation>>> playerSources;

    private static final Vector[] ANIMATION_VECTORS = new Vector[] {
            new Vector(0, 0, 2),
            new Vector(-1, 0, 2),
            new Vector(-2, 0, 1),
            new Vector(-2, 0, 0),
            new Vector(-2, 0, -1),
            new Vector(-1, 0, -2),
            new Vector(0, 0, -2),
            new Vector(1, 0, -2),
            new Vector(2, 0, -1),
            new Vector(2, 0, 0),
            new Vector(2, 0, 1),
            new Vector(1, 0, 2),
    };

    private final BlockSourceInformation shiftDownSourceInfo;
    private final BlockSourceInformation leftClickSourceInfo;
    private final Collection<TempBlock> animationBlocks;

    public static Vector getRandomAnimationPosition() {
        // return ANIMATION_VECTORS[ThreadLocalRandom.current().nextInt(12)].clone();
        return new Vector();
    }

    public WaterRing(Player player) {
        super(player);

        animationBlocks = new HashSet<>();

        this.shiftDownSourceInfo = new BlockSourceInformation(
                player,
                new VirtualWaterSourceBlock(bPlayer),
                BlockSource.BlockSourceType.WATER,
                ClickType.SHIFT_DOWN
        );
        this.leftClickSourceInfo = new BlockSourceInformation(
                player,
                new VirtualWaterSourceBlock(bPlayer),
                BlockSource.BlockSourceType.WATER,
                ClickType.LEFT_CLICK
        );

        enterFakeSourceBlock();
        start();
    }

    private void printSources() {
        playerSources.forEach((player, sources) -> {
            player.sendMessage("Sources for player " + player.getName() + "...");
            sources.forEach((sourceType, clickTypeWithInfo) -> {
                player.sendMessage("  Source: " + sourceType);
                clickTypeWithInfo.forEach((clickType, info) -> {
                    player.sendMessage("    ClickType: " + clickType + " - Is virtual: " + (info.getBlock() instanceof VirtualWaterSourceBlock));
                });
            });
        });
    }

    private void enterFakeSourceBlock() {
        printSources();
        var sourcesForPlayer = playerSources.computeIfAbsent(player, (_) -> new HashMap<>());
        var sourcesForWater = sourcesForPlayer.computeIfAbsent(BlockSource.BlockSourceType.WATER, (_) -> new HashMap<>());

        sourcesForWater.put(ClickType.SHIFT_DOWN, shiftDownSourceInfo);
        sourcesForWater.put(ClickType.LEFT_CLICK, leftClickSourceInfo);
    }

    @Override
    public void progress() {
        if (player.isDead() || !player.isOnline()) {
            remove();
            return;
        }

        if (!bPlayer.canBendIgnoreBinds(this)) {
            remove();
            return;
        }

        if (getStartTime() + 10000 < System.currentTimeMillis()) {
            remove();
            return;
        }

        if (getCurrentTick() % 20 == 0) {
            printSources();
        }

        displayRing();
    }

    private void clearOldRing() {
        animationBlocks.forEach(TempBlock::revertBlock);
    }

    private void displayRing() {
        clearOldRing();
        Location at = player.getEyeLocation();
        for (int step = 0; step < 12; step++) {
            Vector animationStep = ANIMATION_VECTORS[step];
            at.add(animationStep);
            Levelled waterLevelData = (Levelled) Material.WATER.createBlockData();

            int levelForStep = getWaterLevelForStep(step);
            waterLevelData.setLevel(levelForStep == 7 ? waterLevelData.getMaximumLevel() : levelForStep);
            animationBlocks.add(new TempBlock(at.getBlock(), waterLevelData));
            at.subtract(animationStep);
        }
    }

    private int getWaterLevelForStep(long step) {
        step += getRunningTicks() / 4;
        step %= 12;

        if (step == 0 || step == 2) {
            return 5;
        }
        if (step == 1) {
            return 7;
        }
        return 1;
    }

    private void removeVirtualSources() {
        var sourcesForPlayer = playerSources.get(player);
        if (sourcesForPlayer == null) return;

        var waterSources = sourcesForPlayer.get(BlockSource.BlockSourceType.WATER);
        if (waterSources == null) return;

        waterSources.remove(ClickType.LEFT_CLICK, leftClickSourceInfo);
        waterSources.remove(ClickType.SHIFT_DOWN, shiftDownSourceInfo);
    }

    @Override
    public void remove() {
        bPlayer.addCooldown(this);
        clearOldRing();
        removeVirtualSources();
        printSources();
        super.remove();
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
        try {
            Field playerSourcesField = BlockSource.class.getDeclaredField("playerSources");
            playerSourcesField.setAccessible(true);
            playerSources = (Map<Player, Map<BlockSource.BlockSourceType, Map<ClickType, BlockSourceInformation>>>) playerSourcesField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
