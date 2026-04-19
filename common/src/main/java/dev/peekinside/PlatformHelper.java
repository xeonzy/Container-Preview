package dev.peekinside;

import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface PlatformHelper {
    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    Path getConfigDir();

    KeyMapping registerKeyBinding(KeyMapping keyMapping);

    boolean isKeyDown(KeyMapping keyMapping);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToServer(CustomPacketPayload payload);

    boolean canSendToServer(CustomPacketPayload.Type<?> type);
}
