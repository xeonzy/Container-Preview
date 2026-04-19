package dev.peekinside.neoforge;

import dev.peekinside.PeekInside;
import dev.peekinside.PeekConstants;
import dev.peekinside.PeekKeys;
import dev.peekinside.network.EnderChestContentsPayload;
import dev.peekinside.network.PocketMachineContentsPayload;
import dev.peekinside.network.RequestEnderChestPayload;
import dev.peekinside.network.RequestPocketMachinePayload;
import dev.peekinside.config.PeekFallbackConfigScreen;
import dev.peekinside.config.YaclScreenProvider;
import dev.peekinside.provider.EnderChestProvider;
import dev.peekinside.provider.PocketMachinesCompat;
import dev.peekinside.provider.PocketMachinesProvider;
import dev.peekinside.render.PreviewTooltipComponent;
import dev.peekinside.render.PreviewTooltipData;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;

@Mod(PeekConstants.MOD_ID)
public class PeekInsideNeoForge {
    public PeekInsideNeoForge(IEventBus modEventBus) {
        PeekInside.init();
        modEventBus.addListener(this::onRegisterKeyMappings);
        modEventBus.addListener(this::onRegisterPayloadHandlers);
        modEventBus.addListener(this::onRegisterTooltipComponents);
        
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> (client, parent) -> {
            if (YaclScreenProvider.isAvailable()) {
                return YaclScreenProvider.create(parent);
            }
            return PeekFallbackConfigScreen.create(parent);
        });
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PeekKeys.PREVIEW);
        event.register(PeekKeys.DETAIL);
        event.register(PeekKeys.LOCK);
    }

    private void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PeekConstants.MOD_ID);
        registrar.playToServer(RequestEnderChestPayload.TYPE, RequestEnderChestPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            PlayerEnderChestContainer enderChest = player.getEnderChestInventory();
            List<ItemStack> slots = new ArrayList<>(enderChest.getContainerSize());
            for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
                slots.add(enderChest.getItem(slot).copy());
            }

            PacketDistributor.sendToPlayer(player, new EnderChestContentsPayload(slots));
        }));
        registrar.playToServer(RequestPocketMachinePayload.TYPE, RequestPocketMachinePayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!ModList.get().isLoaded(PocketMachinesCompat.MOD_ID)) {
                return;
            }

            ServerPlayer player = (ServerPlayer) context.player();
            PocketMachinesCompat.MachineType machineType = PocketMachinesCompat.machineType(payload.machineKey().replace("TG_", "pocket_").toLowerCase(Locale.ROOT));
            if (machineType == null) {
                return;
            }

            PocketMachinesCompat.ResolvedPreview preview = PocketMachinesCompat.resolvePreview(machineType, payload.stackId(), player);
            if (preview == null) {
                return;
            }

            PacketDistributor.sendToPlayer(
                player,
                new PocketMachineContentsPayload(
                    payload.machineKey(),
                    payload.stackId(),
                    preview.label() == null ? "" : preview.label().getString(),
                    preview.columns(),
                    preview.rows(),
                    preview.slots()
                )
            );
        }));
        registrar.playToClient(EnderChestContentsPayload.TYPE, EnderChestContentsPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> EnderChestProvider.updateContents(payload.slots())));
        registrar.playToClient(PocketMachineContentsPayload.TYPE, PocketMachineContentsPayload.STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> PocketMachinesProvider.updateContents(
            payload.machineKey(),
            payload.stackId(),
            payload.label(),
            payload.columns(),
            payload.rows(),
            payload.slots()
        )));
    }

    private void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(PreviewTooltipData.class, PreviewTooltipComponent::new);
    }
}
