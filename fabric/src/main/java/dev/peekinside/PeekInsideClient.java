package dev.peekinside;

import dev.peekinside.api.ContainerProviderRegistry;
import dev.peekinside.api.PeekInsideEntrypoint;
import dev.peekinside.config.PeekConfig;
import dev.peekinside.provider.BundleContentsProvider;
import dev.peekinside.provider.BlankPackProvider;
import dev.peekinside.provider.ComponentContainerProvider;
import dev.peekinside.provider.EnderChestProvider;
import dev.peekinside.provider.PackedChestProvider;
import dev.peekinside.provider.PocketMachinesProvider;
import dev.peekinside.provider.TagContainerProvider;
import dev.peekinside.render.PreviewAnalysisCache;
import dev.peekinside.render.PreviewState;
import dev.peekinside.render.PreviewTooltipComponent;
import dev.peekinside.render.PreviewTooltipData;
import dev.peekinside.network.EnderChestContentsPayload;
import dev.peekinside.network.PocketMachineContentsPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

@SuppressWarnings("null")
public final class PeekInsideClient implements ClientModInitializer {
	private static final String DISABLE_CONFIG_INIT_ENV = "PEEKINSIDE_DISABLE_CONFIG_INIT";
	private static final String DISABLE_KEY_CONFIG_ENV = "PEEKINSIDE_DISABLE_KEY_CONFIG";

	@Override
	public void onInitializeClient() {
		if (!flag(DISABLE_CONFIG_INIT_ENV)) {
			PeekConfig.reload();
		} else {
			PeekInside.LOGGER.warn("Skipping config reload because {} is enabled", DISABLE_CONFIG_INIT_ENV);
		}

		PeekKeys.initialize();

		if (!flag(DISABLE_KEY_CONFIG_ENV)) {
			PeekKeys.applyFromConfig();
			KeyMapping.resetMapping();
		} else {
			PeekInside.LOGGER.warn("Skipping keybinding remap because {} is enabled", DISABLE_KEY_CONFIG_ENV);
		}

		RuntimeContainerCompat.register();
		registerClientLifecycle();
		registerClientReceivers();
		registerProviders();
		registerTooltipComponents();
	}

	private static void registerProviders() {
		ContainerProviderRegistry registry = ContainerProviderRegistry.INSTANCE;

		FabricLoader.getInstance()
			.getEntrypoints("peekinside", PeekInsideEntrypoint.class)
			.forEach(entrypoint -> entrypoint.register(registry));

	}

	private static void registerTooltipComponents() {
		TooltipComponentCallback.EVENT.register(data -> {
			if (data instanceof PreviewTooltipData previewTooltipData) {
				return new PreviewTooltipComponent(previewTooltipData);
			}

			return null;
		});
	}

	private static void registerClientReceivers() {
		ClientPlayNetworking.registerGlobalReceiver(EnderChestContentsPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			EnderChestProvider.updateContents(payload.slots());
		}));
		ClientPlayNetworking.registerGlobalReceiver(PocketMachineContentsPayload.TYPE, (payload, context) -> context.client().execute(() -> {
			PocketMachinesProvider.updateContents(
				payload.machineKey(),
				payload.stackId(),
				payload.label(),
				payload.columns(),
				payload.rows(),
				payload.slots()
			);
		}));
	}

	private static void registerClientLifecycle() {
		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.level == null) {
				PreviewState.clearAll();
				EnderChestProvider.clearCache();
				PocketMachinesProvider.clearCache();
				PreviewAnalysisCache.INSTANCE.clear();
				return;
			}

			PreviewState.refreshLockedPreview();

			if (!(client.screen instanceof AbstractContainerScreen<?>)) {
				PreviewState.clearAll();
			}
		});
	}

	private static boolean flag(String key) {
		return Boolean.parseBoolean(System.getenv(key));
	}
}
