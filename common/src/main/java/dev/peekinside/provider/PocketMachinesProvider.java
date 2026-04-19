package dev.peekinside.provider;

import dev.peekinside.Services;
import dev.peekinside.api.ContainerProvider;
import dev.peekinside.api.PreviewRequest;
import dev.peekinside.api.PreviewResult;
import dev.peekinside.network.RequestPocketMachinePayload;
import dev.peekinside.render.PreviewAnalysisCache;
import dev.peekinside.render.PreviewCache;
import dev.peekinside.render.PreviewState;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("null")
public final class PocketMachinesProvider implements ContainerProvider {
	public static final int PRIORITY = 880;

	private static final Map<String, PreviewResult> CACHED_PREVIEWS = new ConcurrentHashMap<>();
	private static final Set<String> PENDING_REQUESTS = ConcurrentHashMap.newKeySet();

	@Override
	public @Nullable PreviewResult provide(PreviewRequest request) {
		Identifier itemId = BuiltInRegistries.ITEM.getKey(request.stack().getItem());
		if (!PocketMachinesCompat.MOD_ID.equals(itemId.getNamespace()) || request.player() == null) {
			return null;
		}

		PocketMachinesCompat.MachineType machineType = PocketMachinesCompat.machineType(itemId.getPath());
		if (machineType == null) {
			return null;
		}

		String stackId = PocketMachinesCompat.stackId(request.stack(), machineType);
		if (stackId == null) {
			return null;
		}

		PreviewResult preview = CACHED_PREVIEWS.get(cacheKey(machineType.storageKey(), stackId));
		if (preview != null) {
			return preview;
		}

		requestContents(machineType.storageKey(), stackId);
		return null;
	}

	public static void updateContents(String machineKey, String stackId, String label, int columns, int rows, java.util.List<net.minecraft.world.item.ItemStack> slots) {
		String cacheKey = cacheKey(machineKey, stackId);
		CACHED_PREVIEWS.put(
			cacheKey,
			new PreviewResult(java.util.List.copyOf(slots), columns, rows, label.isBlank() ? null : Component.literal(label))
		);
		PENDING_REQUESTS.remove(cacheKey);
		PreviewCache.INSTANCE.clear();
		PreviewAnalysisCache.INSTANCE.clear();
		PreviewState.refreshLockedPreview();
	}

	public static void clearCache() {
		CACHED_PREVIEWS.clear();
		PENDING_REQUESTS.clear();
		PreviewCache.INSTANCE.clear();
		PreviewAnalysisCache.INSTANCE.clear();
	}

	private static void requestContents(String machineKey, String stackId) {
		String cacheKey = cacheKey(machineKey, stackId);
		if (PENDING_REQUESTS.contains(cacheKey) || !Services.PLATFORM.canSendToServer(RequestPocketMachinePayload.TYPE)) {
			return;
		}

		PENDING_REQUESTS.add(cacheKey);
		Services.PLATFORM.sendToServer(new RequestPocketMachinePayload(machineKey, stackId));
	}

	private static String cacheKey(String machineKey, String stackId) {
		return machineKey + "|" + stackId;
	}
}
