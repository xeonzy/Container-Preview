package dev.peekinside.forge;

import dev.peekinside.PeekConstants;
import dev.peekinside.network.EnderChestContentsPayload;
import dev.peekinside.network.PocketMachineContentsPayload;
import dev.peekinside.network.RequestEnderChestPayload;
import dev.peekinside.network.RequestPocketMachinePayload;
import dev.peekinside.provider.EnderChestProvider;
import dev.peekinside.provider.PocketMachinesCompat;
import dev.peekinside.provider.PocketMachinesProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ForgeNetworking {

    private static SimpleChannel CHANNEL;

    public static void init() {
        CHANNEL = ChannelBuilder.named(net.minecraft.resources.Identifier.fromNamespaceAndPath(PeekConstants.MOD_ID, "main"))
                .networkProtocolVersion(1)
                .acceptedVersions((v, s) -> true)
                .simpleChannel();

        int id = 0;
        CHANNEL.messageBuilder(RequestEnderChestPayload.class, id++)
                .encoder((msg, buf) -> RequestEnderChestPayload.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, msg))
                .decoder(buf -> RequestEnderChestPayload.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf))
                .consumerNetworkThread((payload, context) -> {
                    context.setPacketHandled(true);
                    context.enqueueWork(() -> {
                        ServerPlayer player = (ServerPlayer) context.getSender();
                        PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
                        List<ItemStack> slots = new ArrayList<>(enderChest.getContainerSize());
                        for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
                            slots.add(enderChest.getItem(slot).copy());
                        }
                        CHANNEL.send(new EnderChestContentsPayload(slots), PacketDistributor.PLAYER.with(player));
                    });
                })
                .add();

        CHANNEL.messageBuilder(RequestPocketMachinePayload.class, id++)
                .encoder((msg, buf) -> RequestPocketMachinePayload.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, msg))
                .decoder(buf -> RequestPocketMachinePayload.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf))
                .consumerNetworkThread((payload, context) -> {
                    context.setPacketHandled(true);
                    context.enqueueWork(() -> {
                        if (!ModList.get().isLoaded(PocketMachinesCompat.MOD_ID)) {
                            return;
                        }
                        ServerPlayer player = (ServerPlayer) context.getSender();
                        PocketMachinesCompat.MachineType machineType = PocketMachinesCompat.machineType(payload.machineKey().replace("TG_", "pocket_").toLowerCase(Locale.ROOT));
                        if (machineType == null) {
                            return;
                        }
                        PocketMachinesCompat.ResolvedPreview preview = PocketMachinesCompat.resolvePreview(machineType, payload.stackId(), player);
                        if (preview == null) {
                            return;
                        }
                        CHANNEL.send(new PocketMachineContentsPayload(
                                payload.machineKey(),
                                payload.stackId(),
                                preview.label() == null ? "" : preview.label().getString(),
                                preview.columns(),
                                preview.rows(),
                                preview.slots()
                        ), PacketDistributor.PLAYER.with(player));
                    });
                })
                .add();

        CHANNEL.messageBuilder(EnderChestContentsPayload.class, id++)
                .encoder((msg, buf) -> EnderChestContentsPayload.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, msg))
                .decoder(buf -> EnderChestContentsPayload.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf))
                .consumerNetworkThread((payload, context) -> {
                    context.setPacketHandled(true);
                    context.enqueueWork(() -> {
                        EnderChestProvider.updateContents(payload.slots());
                    });
                })
                .add();

        CHANNEL.messageBuilder(PocketMachineContentsPayload.class, id++)
                .encoder((msg, buf) -> PocketMachineContentsPayload.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, msg))
                .decoder(buf -> PocketMachineContentsPayload.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf))
                .consumerNetworkThread((payload, context) -> {
                    context.setPacketHandled(true);
                    context.enqueueWork(() -> {
                        PocketMachinesProvider.updateContents(
                                payload.machineKey(),
                                payload.stackId(),
                                payload.label(),
                                payload.columns(),
                                payload.rows(),
                                payload.slots()
                        );
                    });
                })
                .add();
    }

    public static void sendToServer(Object payload) {
        if (CHANNEL != null) {
            CHANNEL.send(payload, PacketDistributor.SERVER.noArg());
        }
    }

    public static void sendToPlayer(ServerPlayer player, Object payload) {
        if (CHANNEL != null) {
            CHANNEL.send(payload, PacketDistributor.PLAYER.with(player));
        }
    }
}
