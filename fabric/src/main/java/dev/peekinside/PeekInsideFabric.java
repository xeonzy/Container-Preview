package dev.peekinside;

import dev.peekinside.network.EnderChestContentsPayload;
import dev.peekinside.network.PocketMachineContentsPayload;
import dev.peekinside.network.RequestEnderChestPayload;
import dev.peekinside.network.RequestPocketMachinePayload;
import dev.peekinside.provider.PocketMachinesCompat;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public final class PeekInsideFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		PeekInside.init();
		RuntimeContainerCompat.register();
		PayloadTypeRegistry.playC2S().register(RequestEnderChestPayload.TYPE, RequestEnderChestPayload.STREAM_CODEC);
		PayloadTypeRegistry.playC2S().register(RequestPocketMachinePayload.TYPE, RequestPocketMachinePayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(EnderChestContentsPayload.TYPE, EnderChestContentsPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(PocketMachineContentsPayload.TYPE, PocketMachineContentsPayload.STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(RequestEnderChestPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			PlayerEnderChestContainer enderChest = context.player().getEnderChestInventory();
			List<ItemStack> slots = new ArrayList<>(enderChest.getContainerSize());
			for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
				slots.add(enderChest.getItem(slot).copy());
			}

			ServerPlayNetworking.send(context.player(), new EnderChestContentsPayload(slots));
		}));

		ServerPlayNetworking.registerGlobalReceiver(RequestPocketMachinePayload.TYPE, (payload, context) -> context.server().execute(() -> {
			if (!FabricLoader.getInstance().isModLoaded(PocketMachinesCompat.MOD_ID)) {
				return;
			}

			PocketMachinesCompat.MachineType machineType = PocketMachinesCompat.machineType(payload.machineKey().replace("TG_", "pocket_").toLowerCase(java.util.Locale.ROOT));
			if (machineType == null) {
				return;
			}

			PocketMachinesCompat.ResolvedPreview preview = PocketMachinesCompat.resolvePreview(machineType, payload.stackId(), context.player());
			if (preview == null) {
				return;
			}

			ServerPlayNetworking.send(
				context.player(),
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
	}
}
