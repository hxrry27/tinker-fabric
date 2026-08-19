package dev.hxrry.tinker.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


public record TinkerPayload(byte[] data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TinkerPayload> TYPE =
            new CustomPacketPayload.Type<>(Protocol.CHANNEL);

    public static final StreamCodec<FriendlyByteBuf, TinkerPayload> CODEC =
            CustomPacketPayload.codec(TinkerPayload::write, TinkerPayload::read);

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeBytes(data);
    }

    private static TinkerPayload read(FriendlyByteBuf buffer) {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return new TinkerPayload(bytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
