package me.finnbueno.waterRing;

import com.projectkorra.projectkorra.BendingPlayer;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.util.VoxelShape;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class VirtualWaterSourceBlock implements Block {

    private final Player player;

    public VirtualWaterSourceBlock(BendingPlayer bender) {
        this.player = bender.getPlayer();
    }

    private Block getEyeBlock() {
        Location eyeLocation = player.getEyeLocation();
        Vector offset = WaterRing.getRandomAnimationPosition();
        return eyeLocation.add(offset).getBlock();
    }

    private Location getEyeBlockLocation() {
        return getEyeBlock().getLocation();
    }

    @Override
    public byte getData() {
        return 0;
    }

    @Override
    @NotNull
    public BlockData getBlockData() {
        return Material.WATER.createBlockData();
    }

    @Override
    @NotNull
    public Block getRelative(int i, int i1, int i2) {
        return getEyeBlock().getRelative(i, i1, i2);
    }

    @Override
    @NotNull
    public Block getRelative(@NotNull BlockFace blockFace) {
        return getEyeBlock().getRelative(blockFace);
    }

    @Override
    @NotNull
    public Block getRelative(@NotNull BlockFace blockFace, int i) {
        return getEyeBlock().getRelative(blockFace, i);
    }

    @Override
    @NotNull
    public Material getType() {
        return Material.WATER;
    }

    @Override
    public byte getLightLevel() {
        return 0;
    }

    @Override
    public byte getLightFromSky() {
        return 0;
    }

    @Override
    public byte getLightFromBlocks() {
        return 0;
    }

    @Override
    @NotNull
    public World getWorld() {
        return player.getWorld();
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getZ() {
        return 0;
    }

    @Override
    @NotNull
    public Location getLocation() {
        return getEyeBlockLocation();
    }

    @Override
    @NotNull
    public Location getLocation(Location location) {
        Objects.requireNonNull(location).setX(getLocation().getX());
        location.setY(getLocation().getY());
        location.setZ(getLocation().getZ());
        location.setWorld(getLocation().getWorld());
        location.setYaw(getLocation().getYaw());
        location.setPitch(getLocation().getPitch());
        return location;
    }

    @Override
    @NotNull
    public Chunk getChunk() {
        return getLocation().getChunk();
    }

    @Override
    public void setBlockData(@NotNull BlockData blockData) {
    }

    @Override
    public void setBlockData(@NotNull BlockData blockData, boolean b) {
    }

    @Override
    public void setType(@NotNull Material material) {
    }

    @Override
    public void setType(@NotNull Material material, boolean b) {
    }

    @Override
    public BlockFace getFace(@NotNull Block block) {
        return getEyeBlock().getFace(block);
    }

    @Override
    @NotNull
    public BlockState getState() {
        // not an ideal solution
        return getBlockData().createBlockState();
    }

    @Override
    @NotNull
    public Biome getBiome() {
        return getEyeBlock().getBiome();
    }

    @Override
    public void setBiome(@NotNull Biome biome) {

    }

    @Override
    public boolean isBlockPowered() {
        return false;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    @Override
    public boolean isBlockFacePowered(@NotNull BlockFace blockFace) {
        return getEyeBlock().isBlockFacePowered(blockFace);
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(@NotNull BlockFace blockFace) {
        return getEyeBlock().isBlockFaceIndirectlyPowered(blockFace);
    }

    @Override
    public int getBlockPower(@NotNull BlockFace blockFace) {
        return 0;
    }

    @Override
    public int getBlockPower() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isLiquid() {
        return false;
    }

    @Override
    public double getTemperature() {
        return 0;
    }

    @Override
    public double getHumidity() {
        return 0;
    }

    @Override
    @NotNull
    public PistonMoveReaction getPistonMoveReaction() {
        return getEyeBlock().getPistonMoveReaction();
    }

    @Override
    public boolean breakNaturally() {
        return false;
    }

    @Override
    public boolean breakNaturally(ItemStack itemStack) {
        return false;
    }

    @Override
    public boolean applyBoneMeal(@NotNull BlockFace blockFace) {
        return false;
    }

    @Override
    @NotNull
    public Collection<ItemStack> getDrops() {
        return List.of();
    }

    @Override
    @NotNull
    public Collection<ItemStack> getDrops(ItemStack itemStack) {
        return List.of();
    }

    @Override
    @NotNull
    public Collection<ItemStack> getDrops(@NotNull ItemStack itemStack, Entity entity) {
        return List.of();
    }

    @Override
    public boolean isPreferredTool(@NotNull ItemStack itemStack) {
        return false;
    }

    @Override
    public float getBreakSpeed(@NotNull Player player) {
        return 0;
    }

    @Override
    public boolean isPassable() {
        return false;
    }

    @Override
    public RayTraceResult rayTrace(@NotNull Location location, @NotNull Vector vector, double v, @NotNull FluidCollisionMode fluidCollisionMode) {
        return getEyeBlock().rayTrace(location, vector, v, fluidCollisionMode);
    }

    @Override
    @NotNull
    public BoundingBox getBoundingBox() {
        return getEyeBlock().getBoundingBox();
    }

    @Override
    @NotNull
    public VoxelShape getCollisionShape() {
        return getEyeBlock().getCollisionShape();
    }

    @Override
    public boolean canPlace(@NotNull BlockData blockData) {
        return getEyeBlock().canPlace(blockData);
    }

    @Override
    @NotNull
    public String getTranslationKey() {
        return getEyeBlock().getTranslationKey();
    }

    @Override
    public void setMetadata(@NotNull String s, @NotNull MetadataValue metadataValue) {
    }

    @Override
    @NotNull
    public List<MetadataValue> getMetadata(@NotNull String s) {
        return List.of();
    }

    @Override
    public boolean hasMetadata(@NotNull String s) {
        return false;
    }

    @Override
    public void removeMetadata(@NotNull String s, @NotNull Plugin plugin) {
    }
}
