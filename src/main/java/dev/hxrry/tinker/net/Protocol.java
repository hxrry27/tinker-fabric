package dev.hxrry.tinker.net;

import net.minecraft.resources.Identifier;


public final class Protocol {

    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("tinker", "main");

    public static final int PROTOCOL_VERSION = 1;

    // client -> server
    public static final byte HELLO = 0x01;
    public static final byte TOGGLE_TINKER = 0x02;
    public static final byte CYCLE_PROPERTY = 0x03;

    // server -> client
    public static final byte HELLO_ACK = (byte) 0x81;
    public static final byte STATE = (byte) 0x82;

    public static final byte MODE_OFF = 0;
    public static final byte MODE_ON = 1;
    public static final byte MODE_TOGGLE = 2;

    public static final byte DIRECTION_BACKWARD = 0;
    public static final byte DIRECTION_FORWARD = 1;

    public static final int MAX_PAYLOAD_BYTES = 64;

    public static final int MAX_MESSAGES_PER_SECOND = 20;

    private Protocol() {
    }
}
