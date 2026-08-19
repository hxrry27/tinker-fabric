package dev.hxrry.tinker.client;

public final class ClientState {

    private static boolean acknowledged;
    private static boolean supported;
    private static boolean tinkerEnabled;

    private ClientState() {
    }

    // backend ack *and* we agree on version
    public static boolean available() {
        return acknowledged && supported;
    }

    // backend ack but we disagree on version
    public static boolean unsupported() {
        return acknowledged && !supported;
    }

    // backend ack, regardless of version support
    public static boolean acknowledged() {
        return acknowledged;
    }

    public static boolean tinkerEnabled() {
        return tinkerEnabled;
    }

    static void acknowledge(boolean versionSupported) {
        acknowledged = true;
        supported = versionSupported;
    }

    static boolean tinkerEnabled(boolean enabled) {
        boolean changed = enabled != tinkerEnabled;
        tinkerEnabled = enabled;
        return changed;
    }

    static void reset() {
        acknowledged = false;
        supported = false;
        tinkerEnabled = false;
    }
}
