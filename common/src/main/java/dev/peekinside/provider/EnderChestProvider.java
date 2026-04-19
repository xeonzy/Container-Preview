package dev.peekinside.provider;

import dev.peekinside.Services;
import dev.peekinside.api.ContainerProvider;
import dev.peekinside.api.PreviewRequest;
import dev.peekinside.api.PreviewResult;
import dev.peekinside.render.PreviewAnalysisCache;
import dev.peekinside.network.RequestEnderChestPayload;
import dev.peekinside.render.PreviewCache;
import dev.peekinside.render.PreviewState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("null")
public final class EnderChestProvider implements ContainerProvider {
	public static final int PRIORITY = 900;
	private static final int COLUMNS = 9;
	private static final int ROWS = 3;
	private static final Component LABEL = Component.translatable("peekinside.ender_chest");
	private static final Identifier POCKET_ENDER_CHEST_ID = Identifier.fromNamespaceAndPath("pocketmachines", "pocket_ender_chest");

	private static volatile @Nullable PreviewResult cachedPreview;
	private static volatile boolean requestPending;

	@Override
	public @Nullable PreviewResult provide(PreviewRequest request) {
		if (request.stack().is(Items.ENDER_CHEST)) {
			return cachedPreview != null ? cachedPreview : requestContents();
		}

		boolean isPocketEChest = BuiltInRegistries.ITEM.getOptional(POCKET_ENDER_CHEST_ID)
				.map(request.stack()::is)
				.orElse(false);

		if (isPocketEChest) {
			return cachedPreview != null ? cachedPreview : requestContents();
		}

		return null;
	}

	private static @Nullable PreviewResult requestContents() {
		if (!requestPending && Services.PLATFORM.canSendToServer(RequestEnderChestPayload.TYPE)) {
			requestPending = true;
			Services.PLATFORM.sendToServer(RequestEnderChestPayload.INSTANCE);
		}
		return null;
	}

	public static void updateContents(List<ItemStack> slots) {
		cachedPreview = new PreviewResult(normalizeSlots(slots), COLUMNS, ROWS, LABEL);
		requestPending = false;
		PreviewCache.INSTANCE.clear();
		PreviewAnalysisCache.INSTANCE.clear();
		PreviewState.refreshLockedPreview();
	}

	public static void clearCache() {
		cachedPreview = null;
		requestPending = false;
		PreviewCache.INSTANCE.clear();
		PreviewAnalysisCache.INSTANCE.clear();
	}


	private static List<ItemStack> normalizeSlots(List<ItemStack> slots) {
		List<ItemStack> normalized = new ArrayList<>(COLUMNS * ROWS);
		for (int index = 0; index < COLUMNS * ROWS; index++) {
			if (index < slots.size()) {
				normalized.add(slots.get(index).copy());
			} else {
				normalized.add(ItemStack.EMPTY);
			}
		}

		return List.copyOf(normalized);
	}
}
