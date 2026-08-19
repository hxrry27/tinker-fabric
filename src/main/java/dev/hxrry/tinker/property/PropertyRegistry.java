package dev.hxrry.tinker.property;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public final class PropertyRegistry {

    public static final Set<Block> LIT_BLOCKS = Set.of(Blocks.FURNACE, Blocks.BLAST_FURNACE,
            Blocks.SMOKER, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);

    private static final List<String> CONNECTING_FACES =
            List.of("north", "east", "south", "west", "up", "down");

    private static final List<String> WALL_FACES = List.of("north", "east", "south", "west");

    private static final List<TinkerProperty> PROPERTIES = build();

    private PropertyRegistry() {
    }

    public static List<TinkerProperty> all() {
        return PROPERTIES;
    }

    private static List<TinkerProperty> build() {
        List<TinkerProperty> properties = new ArrayList<>();

        properties.add(StateProperty.enums(Category.STAIRS, "shape",
                StairsShape.class, is(StairBlock.class)));

        properties.add(StateProperty.bool(Category.DOORS, "open", is(DoorBlock.class)));
        properties.add(StateProperty.enums(Category.DOORS, "hinge",
                DoorHingeSide.class, is(DoorBlock.class)));

        properties.add(StateProperty.bool(Category.TRAPDOORS, "open", is(TrapDoorBlock.class)));

        properties.add(StateProperty.bool(Category.FENCE_GATES, "open", is(FenceGateBlock.class)));

        properties.add(StateProperty.bool(Category.BARRELS, "open", is(BarrelBlock.class)));

        properties.add(StateProperty.bool(Category.LIT_BLOCKS, "lit",
                state -> LIT_BLOCKS.contains(state.getBlock())));

        for (final String face : CONNECTING_FACES) {
            properties.add(StateProperty.bool(Category.CONNECTING, face, not(WallBlock.class)));
        }

        for (final String face : WALL_FACES) {
            properties.add(StateProperty.enums(Category.WALLS, face,
                    WallSide.class, is(WallBlock.class)));
        }
        properties.add(StateProperty.bool(Category.WALLS, "up", is(WallBlock.class)));

        // op based stuff

        properties.add(StateProperty.ints(Category.CROPS, "age", any()));

        properties.add(StateProperty.ints(Category.COMPOSTERS, "level",
                state -> state.getBlock() == Blocks.COMPOSTER));

        properties.add(StateProperty.ints(Category.CAULDRONS, "level",
                PropertyRegistry::isCauldron));

        properties.add(StateProperty.ints(Category.BEEHIVES, "honey_level", any()));

        properties.add(StateProperty.ints(Category.CANDLES, "count", "candles", any()));

        properties.add(StateProperty.ints(Category.SEA_PICKLES, "count", "pickles", any()));

        properties.add(StateProperty.ints(Category.SNOW, "layers", any()));

        properties.add(StateProperty.ints(Category.TURTLE_EGGS, "count", "eggs", any()));

        properties.add(StateProperty.ints(Category.RESPAWN_ANCHORS, "charges", any()));

        properties.add(StateProperty.bool(Category.PISTONS, "extended", is(PistonBaseBlock.class)));

        // powerable

        properties.add(StateProperty.bool(Category.REDSTONE, "powered", any()));
        properties.add(StateProperty.ints(Category.REDSTONE, "power", any()));
        properties.add(StateProperty.enums(Category.REDSTONE, "mode",
                ComparatorMode.class, is(ComparatorBlock.class)));
        properties.add(StateProperty.ints(Category.REDSTONE, "delay", is(RepeaterBlock.class)));

        return List.copyOf(properties);
    }

    private static java.util.function.Predicate<BlockState> any() {
        return state -> true;
    }

    private static java.util.function.Predicate<BlockState> is(Class<? extends Block> type) {
        return state -> type.isInstance(state.getBlock());
    }

    private static java.util.function.Predicate<BlockState> not(Class<? extends Block> type) {
        return state -> !type.isInstance(state.getBlock());
    }

    private static boolean isCauldron(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("cauldron");
    }
}
