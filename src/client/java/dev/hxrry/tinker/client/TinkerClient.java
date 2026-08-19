package dev.hxrry.tinker.client;

import net.fabricmc.api.ClientModInitializer;

public final class TinkerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        Keybinds.register();
    }
}
