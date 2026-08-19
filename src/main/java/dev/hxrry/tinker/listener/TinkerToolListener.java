package dev.hxrry.tinker.listener;

import dev.hxrry.tinker.Permissions;
import dev.hxrry.tinker.Tinker;
import dev.hxrry.tinker.config.Messages;
import dev.hxrry.tinker.config.TinkerConfig;
import dev.hxrry.tinker.property.Category;
import dev.hxrry.tinker.property.PropertyResolver;
import dev.hxrry.tinker.property.TinkerProperty;
import dev.hxrry.tinker.session.PlayerSession;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

public final class TinkerToolListener {

    private TinkerToolListener() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(TinkerToolListener::onUse);
        AttackBlockCallback.EVENT.register(TinkerToolListener::onAttack);
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) ->
                !(player instanceof ServerPlayer server) || !isToolActive(server));
    }

    private static InteractionResult onUse(Player player, Level level, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer server)) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !isToolActive(server)) {
            return InteractionResult.PASS;
        }

        Messages messages = Tinker.config().messages();

        if (isSpatial(state)) {
            editFace(server, level, pos, state, hit, messages);
            return InteractionResult.SUCCESS_SERVER;
        }

        List<TinkerProperty> editable = Tinker.resolver().editable(state);
        if (editable.isEmpty()) {
            notEditable(server, state, messages);
            return InteractionResult.SUCCESS_SERVER;
        }

        cycle(server, Tinker.sessions().get(server), level, pos, state, editable, messages);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static InteractionResult onAttack(Player player, Level level, InteractionHand hand,
            BlockPos pos, Direction face) {
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer server)) {
            return InteractionResult.PASS;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !isToolActive(server)) {
            return InteractionResult.PASS;
        }

        if (isSpatial(state)) {
            return InteractionResult.SUCCESS_SERVER;
        }

        Messages messages = Tinker.config().messages();
        List<TinkerProperty> editable = Tinker.resolver().editable(state);
        if (editable.isEmpty()) {
            notEditable(server, state, messages);
            return InteractionResult.SUCCESS_SERVER;
        }

        select(server, Tinker.sessions().get(server), state, editable, messages);
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void notEditable(ServerPlayer player, BlockState state, Messages messages) {
        messages.actionBar(player, "not-editable", Messages.placeholders("block", blockName(state)));
    }

    private static boolean isSpatial(BlockState state) {
        Block block = state.getBlock();
        return block instanceof WallBlock || block instanceof StairBlock || isConnecting(state);
    }

    private static final List<String> FACE_NAMES =
            List.of("north", "east", "south", "west", "up", "down");

    private static boolean isConnecting(BlockState state) {
        if (state.getBlock() instanceof WallBlock) {
            return false;
        }
        for (String face : FACE_NAMES) {
            if (booleanFace(state, face) != null) {
                return true;
            }
        }
        return false;
    }

    private static BooleanProperty booleanFace(BlockState state, String name) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property instanceof BooleanProperty bool ? bool : null;
    }

    private static void editFace(ServerPlayer player, Level level, BlockPos pos, BlockState state,
            BlockHitResult hit, Messages messages) {
        Vec3 point = hit.getLocation();
        double relX = point.x - pos.getX();
        double relY = point.y - pos.getY();
        double relZ = point.z - pos.getZ();

        if (state.getBlock() instanceof StairBlock) {
            editStairs(player, level, pos, state, hit.getDirection(), relX, relY, relZ, messages);
            return;
        }

        TinkerProperty property = hitProperty(state, hit.getDirection(), relX, relY, relZ);
        if (property == null) {
            return;
        }
        apply(player, level, pos, state, property, messages);
    }

    private static void editStairs(ServerPlayer player, Level level, BlockPos pos, BlockState state,
            Direction clicked, double relX, double relY, double relZ, Messages messages) {
        if (!Tinker.config().isCategoryEnabled(Category.STAIRS)) {
            return;
        }
        BlockState updated = StairVoxel.toggle(state, level, pos, clicked, relX, relY, relZ);
        if (updated == null) {
            return;
        }
        write(player, level, pos, updated, "stairs",
                updated.getValue(BlockStateProperties.HORIZONTAL_FACING).name()
                        + "/" + updated.getValue(BlockStateProperties.HALF).name()
                        + "/" + updated.getValue(BlockStateProperties.STAIRS_SHAPE).name(), messages);
    }

    // a fence post is 4px wide, so the middle eighth of the top face is post, not arm
    private static final double POST_RADIUS = 0.125D;
    // a wall post is wider, and its centre is the `up` pillar rather than a dead zone
    private static final double WALL_POST_RADIUS = 0.25D;

    private static TinkerProperty hitProperty(BlockState state, Direction clicked,
            double relX, double relY, double relZ) {
        double offX = Math.abs(relX - 0.5D);
        double offY = Math.abs(relY - 0.5D);
        double offZ = Math.abs(relZ - 0.5D);
        boolean fromTopOrBottom = clicked == Direction.UP || clicked == Direction.DOWN;

        if (state.getBlock() instanceof WallBlock) {
            if (fromTopOrBottom && Math.max(offX, offZ) < WALL_POST_RADIUS) {
                return Tinker.resolver().find(state, Category.WALLS, "up");
            }
            return Tinker.resolver().find(state, Category.WALLS, horizontal(offX, offZ, relX, relZ));
        }

        if (booleanFace(state, "up") != null) {
            // connects vertically too (chorus plant, mushroom stem) - all three axes compete ffs
            if (offY > offX && offY > offZ) {
                return Tinker.resolver().find(state, Category.CONNECTING, relY > 0.5D ? "up" : "down");
            }
            return Tinker.resolver().find(state, Category.CONNECTING, horizontal(offX, offZ, relX, relZ));
        }
        if (fromTopOrBottom && Math.max(offX, offZ) < POST_RADIUS) {
            return null;
        }
        return Tinker.resolver().find(state, Category.CONNECTING, horizontal(offX, offZ, relX, relZ));
    }

    private static BlockState sanitise(BlockState state) {
        if (!(state.getBlock() instanceof WallBlock) || state.getValue(BlockStateProperties.UP)) {
            return state;
        }
        for (var side : WALL_SIDES) {
            if (state.getValue(side) != WallSide.NONE) {
                return state;
            }
        }
        return state.setValue(BlockStateProperties.UP, true);
    }

    private static final List<net.minecraft.world.level.block.state.properties.EnumProperty<WallSide>>
            WALL_SIDES = List.of(BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL,
                    BlockStateProperties.SOUTH_WALL, BlockStateProperties.WEST_WALL);

    private static String horizontal(double offX, double offZ, double relX, double relZ) {
        if (offX > offZ) {
            return relX > 0.5D ? "east" : "west";
        }
        return relZ > 0.5D ? "south" : "north";
    }

    private static void select(ServerPlayer player,
            PlayerSession session,
            BlockState state,
            List<TinkerProperty> editable,
            Messages messages) {
        int current = PropertyResolver.indexOf(editable, session.selected(state.getBlock()));

        TinkerProperty next = editable.get(Math.floorMod(current + 1, editable.size()));
        session.select(state.getBlock(), next.key());

        Tinker.channel().sendState(player);
        messages.actionBar(player, "selected", Messages.placeholders(
                "block", blockName(state),
                "property", next.id(),
                "value", next.render(state)));
    }

    private static void cycle(ServerPlayer player,
            PlayerSession session,
            Level level,
            BlockPos pos,
            BlockState state,
            List<TinkerProperty> editable,
            Messages messages) {
        int index = PropertyResolver.indexOf(editable, session.selected(state.getBlock()));
        TinkerProperty property = editable.get(index < 0 ? 0 : index);
        if (index < 0) {
            session.select(state.getBlock(), property.key());
        }
        apply(player, level, pos, state, property, messages);
    }

    private static void apply(ServerPlayer player, Level level, BlockPos pos, BlockState state,
            TinkerProperty property, Messages messages) {
        BlockState updated;
        try {
            updated = property.cycle(state, 1);
        } catch (IllegalArgumentException e) {
            Tinker.LOGGER.debug("Refused an invalid value for {}", property.key(), e);
            notEditable(player, state, messages);
            return;
        }

        write(player, level, pos, updated, property.id(), property.render(updated), messages);
    }

    private static void write(ServerPlayer player, Level level, BlockPos pos, BlockState updated,
            String property, String value, Messages messages) {
        BlockState finalState = sanitise(updated);
        level.setBlock(pos, finalState, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        messages.actionBar(player, "cycled", Messages.placeholders(
                "block", blockName(updated),
                "property", property,
                "value", value));
    }

    public static boolean isToolActive(ServerPlayer player) {
        PlayerSession session = Tinker.sessions().peek(player);
        if (session == null || !session.tinkerMode()) {
            return false;
        }
        if (!Tinker.channel().isListening(player)) {
            return false;
        }
        if (!Permissions.canUse(player)) {
            return false;
        }
        TinkerConfig config = Tinker.config();
        if (!config.requireToolItem()) {
            return true;
        }
        return player.getMainHandItem().is(config.toolItem());
    }

    private static String blockName(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()
                .toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
