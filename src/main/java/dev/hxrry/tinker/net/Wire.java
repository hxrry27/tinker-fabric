package dev.hxrry.tinker.net;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;


public final class Wire {

    private Wire() {
    }

    public record HelloAck(int protocolVersion, boolean supported) {
    }

    public record State(boolean tinkerEnabled, String selectedProperty, boolean hasTinkerPermission) {
    }

    public static byte[] hello(int protocolVersion) {
        return write(out -> {
            out.writeByte(Protocol.HELLO);
            out.writeInt(protocolVersion);
        });
    }

    public static byte[] toggle(byte mode) {
        return write(out -> {
            out.writeByte(Protocol.TOGGLE_TINKER);
            out.writeByte(mode);
        });
    }

    public static byte[] cycle(byte direction) {
        return write(out -> {
            out.writeByte(Protocol.CYCLE_PROPERTY);
            out.writeByte(direction);
        });
    }

    public static byte[] helloAck(int protocolVersion, boolean supported) {
        return write(out -> {
            out.writeByte(Protocol.HELLO_ACK);
            out.writeInt(protocolVersion);
            out.writeBoolean(supported);
        });
    }

    public static byte[] state(boolean tinkerEnabled, String selectedProperty,
            boolean hasTinkerPermission) {
        return write(out -> {
            out.writeByte(Protocol.STATE);
            out.writeBoolean(tinkerEnabled);
            out.writeBoolean(selectedProperty != null);
            if (selectedProperty != null) {
                out.writeUTF(selectedProperty);
            }
            out.writeBoolean(hasTinkerPermission);
        });
    }

    public static HelloAck readHelloAck(DataInputStream in) throws IOException {
        return new HelloAck(in.readInt(), in.readBoolean());
    }

    public static State readState(DataInputStream in) throws IOException {
        boolean enabled = in.readBoolean();
        String property = in.readBoolean() ? in.readUTF() : null;
        return new State(enabled, property, in.readBoolean());
    }

    private interface Body {
        void write(DataOutputStream out) throws IOException;
    }

    private static byte[] write(Body body) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            body.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);  // a byte array cannot fail to be written to
        }
        return bytes.toByteArray();
    }
}
