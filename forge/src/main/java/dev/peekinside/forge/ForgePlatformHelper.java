package dev.peekinside.forge;

import dev.peekinside.PlatformHelper;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class ForgePlatformHelper implements PlatformHelper {
    @Override
    public String getPlatformName() {
        return "Forge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return net.minecraftforge.fml.ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return false; // Stub
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public KeyMapping registerKeyBinding(KeyMapping keyMapping) {
        return keyMapping;
    }

    @Override
    public boolean isKeyDown(KeyMapping mapping) {
        return mapping.isDown();
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ForgeNetworking.sendToServer(payload);
    }

    @Override
    public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
        return true; // Simple stub for now
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        ForgeNetworking.sendToPlayer(player, payload);
    }
}
