package me.finnbueno.waterRing;

import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.ComboAbility;
import com.projectkorra.projectkorra.ability.CoreAbility;
import com.projectkorra.projectkorra.ability.WaterAbility;
import com.projectkorra.projectkorra.ability.util.ComboManager;
import com.projectkorra.projectkorra.attribute.Attribute;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.BlockSource;
import com.projectkorra.projectkorra.util.BlockSourceInformation;
import com.projectkorra.projectkorra.util.ClickType;
import com.projectkorra.projectkorra.util.TempBlock;
import com.projectkorra.projectkorra.waterbending.*;
import com.projectkorra.projectkorra.waterbending.util.WaterSourceGrabber;
import me.finnbueno.waterRing.consumption.ConsumptionConfiguration;
import me.finnbueno.waterRing.consumption.ConsumptionConfigurationManager;
import me.finnbueno.waterRing.utils.AbilityFinder;
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

    private static final int MAX_AMMUNITION = 20;
    /*
        Per player, per type of source (water, earth, etc.), per clicking type (shift or click), track a block
    */
    private static Map<Player, Map<BlockSource.BlockSourceType, Map<ClickType, BlockSourceInformation>>> playerSources;

    private static final ConsumptionConfigurationManager CONSUMPTION_CONFIGURATION_MANAGER =
            new ConsumptionConfigurationManager(ConfigManager.defaultConfig, new AbilityFinder());

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

    private final Collection<TempBlock> animationBlocks;
    private final BossBar bossBar;
    private final Map<WaterAbility, Integer> refundableConsumers;
    private final WaterSourceGrabber sourceGrabber;
    private State state;
    private Block ringBlockOnCrosshair;
    private BlockSourceInformation leftClickSourceInfo;
    private BlockSourceInformation shiftDownSourceInfo;

    private int ammunition;

    @Attribute(Attribute.COOLDOWN)
    private long cooldown;

    public static Block getRingBlockOnCrosshair(Player player) {
        return player.getEyeLocation().add(ANIMATION_VECTORS[getLookingAtIndex(player) % 12]).getBlock();
    }

    private static int getLookingAtIndex(Player player) {
        float yaw = player.getEyeLocation().getYaw() + 180 + 15;
        if (yaw < 0) {
            yaw = 360 + yaw;
        }
        return (((int) Math.floor(yaw / 30)) + 6) % 12;
    }

    public static boolean isSourcedFromWaterRing(WaterAbility waterAbility) {
        ConsumptionConfiguration configuration = CONSUMPTION_CONFIGURATION_MANAGER.getConfiguration(waterAbility.getClass());
        if (configuration == null) {
            return false;
        }
        Block block = configuration.getSourceBlock(waterAbility);
        return block instanceof VirtualWaterSourceBlock;
    }

    public static boolean attemptAmmunitionConsumption(WaterAbility waterAbility) {
        ConsumptionConfiguration configuration;
        VirtualWaterSourceBlock virtualWaterSourceBlock;
        try {
            configuration = CONSUMPTION_CONFIGURATION_MANAGER.getConfiguration(waterAbility.getClass());
            if (configuration == null) throw new NoSuchElementException();
            virtualWaterSourceBlock = (VirtualWaterSourceBlock) configuration.getSourceBlock(waterAbility);
        } catch (ClassCastException | NoSuchElementException e) {
            throw new IllegalArgumentException("Provided ability was not sourced from a WaterRing instance");
        }

        WaterRing waterRing = virtualWaterSourceBlock.getAssociatedWaterRing();
        if (
            configuration.getUses() > waterRing.getAmmunitionLeft() ||
            (configuration.getUses() == -1 && waterRing.getAmmunitionLeft() != MAX_AMMUNITION)) {
            return false;
        }
        waterRing.decreaseUses(configuration.getUses());
        if (configuration.isRefundable()) {
            waterRing.registerRefundableAmmunition(waterAbility, configuration.getUses());
        }
        return true;
    }

    public static void attemptAmmunitionRefund(WaterAbility waterAbility) {
        Player player = waterAbility.getPlayer();
        WaterRing waterRing = CoreAbility.getAbility(player, WaterRing.class);
        if (waterRing == null) {
            return;
        }

        waterRing.refundAmmunition(waterAbility);
    }

    public WaterRing(Player player) {
        this(player, null);
    }

    public WaterRing(Player player, Block sourceBlock) {
        super(player);

        this.ammunition = ConfigManager.getConfig().getInt("ExtraAbilities.FinnBueno.WaterRing.MaxAmmunition");
        this.cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.FinnBueno.WaterRing.Cooldown");

        this.refundableConsumers = new HashMap<>();

        this.animationBlocks = new HashSet<>();

        this.bossBar = Bukkit.createBossBar("Water Ammunition Left", BarColor.BLUE, BarStyle.SEGMENTED_10);
        this.bossBar.addPlayer(player);

        if (sourceBlock == null) {
            this.state = State.ACTIVE;
            this.sourceGrabber = null;
        } else {
            this.state = State.SETUP;
            this.sourceGrabber = new WaterSourceGrabber(player, sourceBlock.getLocation());
        }

        refreshVirtualSources(getRingBlockOnCrosshair(player));
        start();
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

        switch (this.state) {
            case SETUP -> progressSetup();
            case ACTIVE -> progressActive();
        }
    }

    private void progressSetup() {
        this.sourceGrabber.progress();
        if (this.sourceGrabber.getState() == WaterSourceGrabber.AnimationState.FINISHED) {
            this.state = State.ACTIVE;
        } else if (this.sourceGrabber.getState() == WaterSourceGrabber.AnimationState.FAILED) {
            remove();
        }
    }

    private void progressActive() {
        if (this.ammunition <= 0 && this.refundableConsumers.isEmpty()) {
            remove();
            return;
        }

        Block currentRingBlockOnCrosshair = getRingBlockOnCrosshair(player);
        if (this.ringBlockOnCrosshair != currentRingBlockOnCrosshair) {
            refreshVirtualSources(currentRingBlockOnCrosshair);
        }

        this.bossBar.setProgress(Math.clamp(this.ammunition / (double) MAX_AMMUNITION, 0, 1));
        displayRing();
    }

    private void refreshVirtualSources(Block block) {
        var sourcesForPlayer = playerSources.computeIfAbsent(player, (_) -> new HashMap<>());
        var sourcesForWater = sourcesForPlayer.computeIfAbsent(BlockSource.BlockSourceType.WATER, (_) -> new HashMap<>());

        this.leftClickSourceInfo = new BlockSourceInformation(
                player,
                new VirtualWaterSourceBlock(this, player, block),
                BlockSource.BlockSourceType.WATER,
                ClickType.SHIFT_DOWN
        );
        this.shiftDownSourceInfo = new BlockSourceInformation(
                player,
                new VirtualWaterSourceBlock(this, player, block),
                BlockSource.BlockSourceType.WATER,
                ClickType.LEFT_CLICK
        );

        sourcesForWater.put(ClickType.SHIFT_DOWN, leftClickSourceInfo);
        sourcesForWater.put(ClickType.LEFT_CLICK, shiftDownSourceInfo);

        this.ringBlockOnCrosshair = block;
    }

    private void displayRing() {
        animationBlocks.forEach(TempBlock::revertBlock);

        int lookingAtIndex = getLookingAtIndex(player);
        Location at = player.getEyeLocation();
        for (int step = 0; step < 12; step++) {
            Vector animationStep = ANIMATION_VECTORS[step];
            at.add(animationStep);
            Levelled waterLevelData = (Levelled) Material.WATER.createBlockData();

            int levelForStep = getWaterLevelForAnimationBlock(step);
            waterLevelData.setLevel(levelForStep == 7 ? waterLevelData.getMaximumLevel() : levelForStep);
            int toTheLeft = lookingAtIndex == 0 ? 11 : lookingAtIndex - 1;
            int toTheRight = (lookingAtIndex + 1) % 12;
            if (step != lookingAtIndex && step != toTheRight && step != toTheLeft) {
                animationBlocks.add(new TempBlock(at.getBlock(), waterLevelData));
            }
            at.subtract(animationStep);
        }
    }

    private int getWaterLevelForAnimationBlock(long index) {
        index += getRunningTicks() / 2;
        index %= 12;

        if (index == 0 || index == 2) {
            return 5;
        }
        if (index == 1) {
            return 7;
        }
        return 1;
    }

    public void decreaseUses(int amount) {
        if (amount == -1) {
            this.ammunition = 0;
        } else {
            this.ammunition -= amount;
        }
    }

    private void registerRefundableAmmunition(WaterAbility waterAbility, int charges) {
        this.refundableConsumers.put(waterAbility, charges);
    }

    private void refundAmmunition(WaterAbility waterAbility) {
        if (!this.refundableConsumers.containsKey(waterAbility)) {
            return;
        }
        int ammunitionRefunded = this.refundableConsumers.remove(waterAbility);
        if (ammunitionRefunded == -1) {
            ammunitionRefunded = MAX_AMMUNITION;
        }
        this.ammunition += ammunitionRefunded;
    }

    @Override
    public void remove() {
        super.remove();
        bPlayer.addCooldown(this);

        bossBar.removeAll();
        animationBlocks.forEach(TempBlock::revertBlock);
        removeVirtualSources();
    }

    private void removeVirtualSources() {
        var sourcesForPlayer = playerSources.get(player);
        if (sourcesForPlayer == null) return;

        var waterSources = sourcesForPlayer.get(BlockSource.BlockSourceType.WATER);
        if (waterSources == null) return;

        waterSources.remove(ClickType.LEFT_CLICK, leftClickSourceInfo);
        waterSources.remove(ClickType.SHIFT_DOWN, shiftDownSourceInfo);
    }

    private int getAmmunitionLeft() {
        return this.ammunition;
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
        return cooldown;
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

        CONSUMPTION_CONFIGURATION_MANAGER.addDefaults();
        // This needs to be called AFTER all other moves have been loaded into ProjectKorra
        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, CONSUMPTION_CONFIGURATION_MANAGER::load, 1);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.FinnBueno.WaterRing.MaxAmmunition", 20);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.FinnBueno.WaterRing.Cooldown", 5000);
        ConfigManager.defaultConfig.save();
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
        WaterSpoutWave wave = CoreAbility.getAbility(player, WaterSpoutWave.class);
        if (wave != null) {
            wave.remove();
            return new WaterRing(player);
        }

        WaterSpout spout = CoreAbility.getAbility(player, WaterSpout.class);
        if (spout != null) {
            return new WaterRing(player);
        }

        Block sourceBlock = BlockSource.getWaterSourceBlock(player, 10, true, true, true);
        if (sourceBlock != null) {
            return new WaterRing(player, sourceBlock);
        }
        return null;
    }

    @Override
    public ArrayList<ComboManager.AbilityInformation> getCombination() {
        return new ArrayList<>(Arrays.asList(
                new ComboManager.AbilityInformation("Torrent", ClickType.LEFT_CLICK),
                new ComboManager.AbilityInformation("WaterManipulation", ClickType.LEFT_CLICK),
                new ComboManager.AbilityInformation("WaterManipulation", ClickType.LEFT_CLICK)
        ));
    }

    private enum State {
        SETUP, ACTIVE
    }
}
