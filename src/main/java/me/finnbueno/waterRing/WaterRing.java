package me.finnbueno.waterRing;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.BlockSourceInformation;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.SurgeWall;
import com.projectkorra.projectkorra.waterbending.SurgeWave;
import com.projectkorra.projectkorra.waterbending.Torrent;
import com.projectkorra.projectkorra.waterbending.WaterManipulation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.*;

public class WaterRing extends WaterAbility implements AddonAbility, ComboAbility {

    /*
    Per player, per type of source (water, earth, etc.), per clicking type (shift or click), track a block
     */
    private static Map<Player, Map<BlockSource.BlockSourceType, Map<ClickType, BlockSourceInformation>>> playerSources;

    // TODO for later
    // When an ability ends from which the water could be re-used (like SurgeWall), perhaps have
    // the water return to the ring using AbilityEndEvent. This way, it can REALLY become re-usable
    public static final Map<Class<? extends WaterAbility>, ConsumptionConfiguration<? extends WaterAbility>> CONSUMPTION_CONFIGURATION = Map.of(
                Torrent.class, ConsumptionConfiguration.TORRENT,
            WaterManipulation.class, ConsumptionConfiguration.WATER_MANIPULATION,
            SurgeWave.class, ConsumptionConfiguration.SURGE_WAVE,
            SurgeWall.class, ConsumptionConfiguration.SURGE_WALL
    );

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
    private final BossBar bossBar;
    private final Map<WaterAbility, Integer> refundableConsumptions;
    private int usesLeft;

    public static Vector getRandomAnimationPosition(Player player) {
        return ANIMATION_VECTORS[(getLookingAtIndex(player) + 6) % 12].clone();
    }

    public static void attemptConsumption(WaterAbility waterAbility) {
        // unfortunately there is no way to make this part work with generics
        ConsumptionConfiguration configuration = CONSUMPTION_CONFIGURATION.get(waterAbility.getClass());
        if (configuration == null) {
            return;
        }
        Block block = configuration.getSourceBlock(waterAbility);

        if (block instanceof VirtualWaterSourceBlock virtualWaterSourceBlock) {
            WaterRing waterRing = virtualWaterSourceBlock.getAssociatedWaterRing();
            waterRing.decreaseUses(configuration.getUses());
            if (configuration.isRefundable()) {
                waterRing.registerRefundableConsumption(waterAbility, configuration.getUses());
            }
        }
    }

    public static void attemptRefund(WaterAbility waterAbility) {
        Player player = waterAbility.getPlayer();
        WaterRing waterRing = CoreAbility.getAbility(player, WaterRing.class);
        if (waterRing == null) {
            return;
        }

        waterRing.refundAbility(waterAbility);
    }

    public WaterRing(Player player) {
        super(player);

        this.usesLeft = 20;

        this.refundableConsumptions = new HashMap<>();

        this.animationBlocks = new HashSet<>();

        this.bossBar = Bukkit.createBossBar("Water Ammunition Left", BarColor.BLUE, BarStyle.SEGMENTED_10);
        this.bossBar.addPlayer(player);

        this.shiftDownSourceInfo = new BlockSourceInformation(
                player,
                new VirtualWaterSourceBlock(this, player),
                BlockSource.BlockSourceType.WATER,
                ClickType.SHIFT_DOWN
        );
        this.leftClickSourceInfo = new BlockSourceInformation(
                player,
                new VirtualWaterSourceBlock(this, player),
                BlockSource.BlockSourceType.WATER,
                ClickType.LEFT_CLICK
        );

        enterFakeSourceBlock();
        start();
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

        if (this.usesLeft <= 0) {
            remove();
            return;
        }

        if (getCurrentTick() % 20 == 0) {
            printSources();
        }

        updateBossBar();
        displayRing();
    }

    private void updateBossBar() {
        this.bossBar.setProgress(usesLeft / 20.0);
    }

    public void decreaseUses(int amount) {
        this.usesLeft -= amount;
    }

    private void registerRefundableConsumption(WaterAbility waterAbility, int charges) {
        this.refundableConsumptions.put(waterAbility, charges);
    }

    private void refundAbility(WaterAbility waterAbility) {
        if (!refundableConsumptions.containsKey(waterAbility)) {
            return;
        }
        this.usesLeft += refundableConsumptions.get(waterAbility);
    }

    private void clearOldRing() {
        animationBlocks.forEach(TempBlock::revertBlock);
    }

    private void displayRing() {

        int lookingAtIndex = getLookingAtIndex(player);

        clearOldRing();
        Location at = player.getEyeLocation();
        for (int step = 0; step < 12; step++) {
            Vector animationStep = ANIMATION_VECTORS[step];
            at.add(animationStep);
            Levelled waterLevelData = (Levelled) Material.WATER.createBlockData();

            int levelForStep = getWaterLevelForStep(step);
            waterLevelData.setLevel(levelForStep == 7 ? waterLevelData.getMaximumLevel() : levelForStep);
            int toTheLeft = lookingAtIndex == 0 ? 11 : lookingAtIndex - 1;
            int toTheRight = (lookingAtIndex + 1) % 12;
            if (step != lookingAtIndex && step != toTheRight && step != toTheLeft) {
                animationBlocks.add(new TempBlock(at.getBlock(), waterLevelData));
            }
            at.subtract(animationStep);
        }
    }

    private static int getLookingAtIndex(Player player) {
        float yaw = player.getEyeLocation().getYaw() + 180 + 15;
        if (yaw < 0) {
            yaw = 360 + yaw;
        }
        return (((int) Math.floor(yaw / 30)) + 6) % 12;
    }

    private int getWaterLevelForStep(long step) {
        step += getRunningTicks() / 2;
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
        this.bossBar.removeAll();
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
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new WaterRingListener(), ProjectKorra.plugin);
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

    private void printSources() {
        playerSources.forEach((player, sources) -> {
            player.sendMessage("Sources for player " + player.getName() + "...");
            sources.forEach((sourceType, clickTypeWithInfo) -> {
                player.sendMessage("  Source: " + sourceType);
                clickTypeWithInfo.forEach((clickType, info) -> {
                    player.sendMessage("    ClickType: " + clickType + " - Exist: " + (info.getBlock() != null) + " - Is virtual: " + (info.getBlock() instanceof VirtualWaterSourceBlock));
                });
            });
        });
    }
}
