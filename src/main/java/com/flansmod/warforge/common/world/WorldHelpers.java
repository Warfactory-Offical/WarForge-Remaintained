package com.flansmod.warforge.common.world;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class WorldHelpers {
    static final int OFFSET = 8;

    private WorldHelpers() {
    }

    public static boolean isAnyBlock(World world, BlockPos pos, Block... blocks) {
        final Block foundBlock = world.getBlockState(pos).getBlock();
        for (Block block : blocks) {
            if (foundBlock == block) {
                return true;
            }
        }
        return false;
    }

    public static boolean withinRadius(double dist, double radius) {
        return dist <= radius * radius;
    }

    public static boolean withinDistance(double distToLine, double distToDepoA, double distToDepoB,
                                         double radius, double distBetweenDepos) {
        return withinRadius(distToLine, radius * radius) &&
                withinRadius(distToDepoA, radius * radius + distBetweenDepos) &&
                withinRadius(distToDepoB, radius * radius + distBetweenDepos);
    }

    public static int calculateOffset(int var) {
        return var + OFFSET;
    }
}
