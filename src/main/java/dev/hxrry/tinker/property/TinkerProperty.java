package dev.hxrry.tinker.property;

import net.minecraft.world.level.block.state.BlockState;

public interface TinkerProperty {

    Category category();

    String id();

    default String key() {
        return category().key() + "." + id();
    }

    boolean appliesTo(BlockState state);

    String render(BlockState state);

    BlockState cycle(BlockState state, int direction);
}
