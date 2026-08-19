package dev.hxrry.tinker.session;

import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

public final class PlayerSession {

    private boolean tinkerMode;

    private final Map<Block, String> selection = new HashMap<>();

    public boolean tinkerMode() {
        return tinkerMode;
    }

    public void tinkerMode(boolean tinkerMode) {
        this.tinkerMode = tinkerMode;
    }

    public String selected(Block block) {
        return selection.get(block);
    }

    public void select(Block block, String propertyKey) {
        selection.put(block, propertyKey);
    }
}
