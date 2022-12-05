package com.solegendary.reignofnether.building;

// class for static building functions

import com.solegendary.reignofnether.building.buildings.WheatFarm;
import com.solegendary.reignofnether.building.buildings.Graveyard;
import com.solegendary.reignofnether.building.buildings.VillagerHouse;
import com.solegendary.reignofnether.building.buildings.VillagerTower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BuildingUtils {

    // given a string name return a new instance of that building
    public static Building getNewBuilding(String buildingName, Level level, BlockPos pos, Rotation rotation, String ownerName) {
        Building building = null;
        switch(buildingName) {
            case VillagerHouse.buildingName -> building = new VillagerHouse(level, pos, rotation, ownerName);
            case VillagerTower.buildingName -> building = new VillagerTower(level, pos, rotation, ownerName);
            case Graveyard.buildingName -> building = new Graveyard(level, pos, rotation, ownerName);
            case WheatFarm.buildingName -> building = new WheatFarm(level, pos, rotation, ownerName);
        }
        if (building != null)
            building.setLevel(level);
        return building;
    }

    // note originPos may not actually be a part of the building itself
    public static Building findBuilding(List<Building> buildings, BlockPos pos) {
        for (Building building : buildings)
            if (building.originPos.equals(pos) || building.isPosInsideBuilding(pos))
                return building;
        return null;
    }

    public static BlockPos getMinCorner(ArrayList<BuildingBlock> blocks) {
        return new BlockPos(
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getX())).get().getBlockPos().getX(),
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getY())).get().getBlockPos().getY(),
                blocks.stream().min(Comparator.comparing(block -> block.getBlockPos().getZ())).get().getBlockPos().getZ()
        );
    }
    public static BlockPos getMaxCorner(ArrayList<BuildingBlock> blocks) {
        return new BlockPos(
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getX())).get().getBlockPos().getX(),
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getY())).get().getBlockPos().getY(),
                blocks.stream().max(Comparator.comparing(block -> block.getBlockPos().getZ())).get().getBlockPos().getZ()
        );
    }
    public static BlockPos getCentrePos(ArrayList<BuildingBlock> blocks) {
        BlockPos min = getMinCorner(blocks);
        BlockPos max = getMaxCorner(blocks);
        return new BlockPos(
                (float) (min.getX() + max.getX()) / 2,
                (float) (min.getY() + max.getY()) / 2,
                (float) (min.getZ() + max.getZ()) / 2
        );
    }

    public static Vec3i getBuildingSize(ArrayList<BuildingBlock> blocks) {
        BlockPos min = getMinCorner(blocks);
        BlockPos max = getMaxCorner(blocks);
        return new Vec3i(
                max.getX() - min.getX(),
                max.getY() - min.getY(),
                max.getZ() - min.getZ()
        );
    }

    // get BlockPos values with absolute world positions
    public static ArrayList<BuildingBlock> getAbsoluteBlockData(ArrayList<BuildingBlock> staticBlocks, LevelAccessor level, BlockPos originPos, Rotation rotation) {
        ArrayList<BuildingBlock> blocks = new ArrayList<>();

        for (BuildingBlock block : staticBlocks) {
            block = block.rotate(level, rotation);
            BlockPos bp = block.getBlockPos();

            block.setBlockPos(new BlockPos(
                    bp.getX() + originPos.getX(),
                    bp.getY() + originPos.getY() + 1,
                    bp.getZ() + originPos.getZ()
            ));
            blocks.add(block);
        }
        return blocks;
    }

    // returns whether the given pos is part of ANY building in the level
    public static boolean isPosPartOfAnyBuilding(Level level, BlockPos bp, boolean onlyPlacedBlocks) {
        List<Building> buildings;
        if (level.isClientSide())
            buildings = BuildingClientEvents.getBuildings();
        else
            buildings = BuildingServerEvents.getBuildings();

        for (Building building : buildings)
            if (building.isPosPartOfBuilding(bp, onlyPlacedBlocks))
                return true;
        return false;
    }
}
