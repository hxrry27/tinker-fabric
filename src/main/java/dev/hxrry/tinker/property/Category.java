package dev.hxrry.tinker.property;

import java.util.Locale;

public enum Category {

    // safe stuff, non-functional, cosmetic states only
    STAIRS,
    DOORS,
    TRAPDOORS,
    FENCE_GATES,
    BARRELS,
    LIT_BLOCKS,
    CONNECTING,
    WALLS,

    // functional, abusable, unsafe stuff, supported but default disabled
    CROPS,
    COMPOSTERS,
    CAULDRONS,
    BEEHIVES,
    CANDLES,
    SEA_PICKLES,
    SNOW,
    TURTLE_EGGS,
    RESPAWN_ANCHORS,
    PISTONS,
    REDSTONE;

    private final String key = name().toLowerCase(Locale.ROOT);

    public String key() {
        return key;
    }

    public boolean safeTier() {
        return ordinal() <= WALLS.ordinal();
    }
}
