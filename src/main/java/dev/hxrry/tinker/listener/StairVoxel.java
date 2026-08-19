package dev.hxrry.tinker.listener;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public final class StairVoxel {

    private static final double NEAR = 0.25D;
    private static final double FAR = 0.75D;

    private static final int BITS = 8;

    private static final Direction[] FACINGS = {
        Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST
    };

    private static final Map<Block, Map<Integer, BlockState>> CACHE = new HashMap<>();

    private StairVoxel() {
    }

    static BlockState toggle(BlockState current, BlockGetter level, BlockPos pos,
            Direction clicked, double relX, double relY, double relZ) {
        Map<Integer, BlockState> patterns = patterns(current.getBlock(), level, pos);
        int voxel = pattern(current, level, pos);

        BlockState carved = patterns.get(voxel & ~(1 << octant(clicked, relX, relY, relZ, -1)));
        if (carved != null) {
            return preserve(current, carved);
        }
        BlockState grown = patterns.get(voxel | (1 << octant(clicked, relX, relY, relZ, 1)));
        return grown == null ? null : preserve(current, grown);
    }

    private static int octant(Direction clicked, double relX, double relY, double relZ, int sign) {
        double nudgeX = clicked.getStepX() * 0.25D * sign;
        double nudgeY = clicked.getStepY() * 0.25D * sign;
        double nudgeZ = clicked.getStepZ() * 0.25D * sign;

        int index = 0;
        if (relZ + nudgeZ <= 0.5D) {
            index |= 1;
        }
        if (relX + nudgeX <= 0.5D) {
            index |= 2;
        }
        if (relY + nudgeY <= 0.5D) {
            index |= 4;
        }
        return index;
    }

    private static BlockState preserve(BlockState from, BlockState to) {
        if (from.hasProperty(BlockStateProperties.WATERLOGGED)
                && to.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return to.setValue(BlockStateProperties.WATERLOGGED,
                    from.getValue(BlockStateProperties.WATERLOGGED));
        }
        return to;
    }

    private static Map<Integer, BlockState> patterns(Block block, BlockGetter level, BlockPos pos) {
        Map<Integer, BlockState> cached = CACHE.get(block);
        if (cached != null) {
            return cached;
        }

        Map<Integer, BlockState> built = new HashMap<>();
        for (Direction facing : FACINGS) {
            for (Half half : Half.values()) {
                for (StairsShape shape : StairsShape.values()) {
                    BlockState candidate = block.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                            .setValue(BlockStateProperties.HALF, half)
                            .setValue(BlockStateProperties.STAIRS_SHAPE, shape);
                    built.putIfAbsent(pattern(candidate, level, pos), candidate);
                }
            }
        }
        Map<Integer, BlockState> patterns = Map.copyOf(built);
        CACHE.put(block, patterns);
        return patterns;
    }

    private static int pattern(BlockState state, BlockGetter level, BlockPos pos) {
        var boxes = state.getCollisionShape(level, pos).toAabbs();
        int voxel = 0;
        for (int index = 0; index < BITS; index++) {
            double x = (index & 2) != 0 ? NEAR : FAR;
            double y = (index & 4) != 0 ? NEAR : FAR;
            double z = (index & 1) != 0 ? NEAR : FAR;
            for (AABB box : boxes) {
                if (box.contains(x, y, z)) {
                    voxel |= 1 << index;
                    break;
                }
            }
        }
        return voxel;
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
