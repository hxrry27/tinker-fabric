package dev.hxrry.tinker;

import dev.hxrry.tinker.property.PropertyResolver;
import dev.hxrry.tinker.property.TinkerProperty;
import dev.hxrry.tinker.session.PlayerSession;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class TinkerService {

    private static final int TARGET_RANGE = 6;

    private TinkerService() {
    }

    public static boolean tinkerMode(ServerPlayer player) {
        PlayerSession session = Tinker.sessions().peek(player);
        return session != null && session.tinkerMode();
    }

    public static boolean setTinkerMode(ServerPlayer player, boolean enabled) {
        if (enabled && (!Permissions.canUse(player) || !Tinker.channel().isListening(player))) {
            return false;
        }
        PlayerSession session = Tinker.sessions().get(player);
        session.tinkerMode(enabled);
        publish(player);
        return enabled;
    }

    public static boolean toggleTinkerMode(ServerPlayer player) {
        return setTinkerMode(player, !tinkerMode(player));
    }

    public static TinkerProperty cycleSelection(ServerPlayer player, boolean forward) {
        if (!Permissions.canUse(player) || !tinkerMode(player)) {
            return null;
        }
        BlockPos pos = targetBlock(player);
        if (pos == null) {
            return null;
        }
        BlockState state = player.level().getBlockState(pos);
        List<TinkerProperty> editable = Tinker.resolver().editable(state);
        if (editable.isEmpty()) {
            return null;
        }

        PlayerSession session = Tinker.sessions().get(player);
        int current = PropertyResolver.indexOf(editable, session.selected(state.getBlock()));
        int step = forward ? 1 : -1;
        TinkerProperty next = editable.get(Math.floorMod(current + step, editable.size()));
        session.select(state.getBlock(), next.key());
        publish(player);
        return next;
    }

    public static String selectedProperty(ServerPlayer player, Block block) {
        PlayerSession session = Tinker.sessions().peek(player);
        return session == null ? null : session.selected(block);
    }

    public static void publish(ServerPlayer player) {
        Tinker.channel().sendState(player);
    }

    public static BlockPos targetBlock(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 reach = eye.add(player.getViewVector(1.0F).scale(TARGET_RANGE));
        HitResult hit = player.level().clip(new ClipContext(eye, reach,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
        return player.level().getBlockState(pos).isAir() ? null : pos;
    }
}
