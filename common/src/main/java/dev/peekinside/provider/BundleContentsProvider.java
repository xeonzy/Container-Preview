package dev.peekinside.provider;

import dev.peekinside.api.ContainerProvider;
import dev.peekinside.api.PreviewRequest;
import dev.peekinside.api.PreviewResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.jspecify.annotations.Nullable;

public final class BundleContentsProvider implements ContainerProvider {
	public static final int PRIORITY = 1000;
	private static final int COLUMNS = 4;

	@Override
	public @Nullable PreviewResult provide(PreviewRequest request) {
		BundleContents bundle = request.stack().get(DataComponents.BUNDLE_CONTENTS);
		if (bundle == null) {
			return null;
		}

		List<ItemStack> items = new ArrayList<>();
		bundle.items().forEach(item -> items.add(item.copy()));

		int rows = Math.max(1, (items.size() + COLUMNS - 1) / COLUMNS);
		while (items.size() < COLUMNS * rows) {
			items.add(ItemStack.EMPTY);
		}

		return new PreviewResult(items, COLUMNS, rows, Component.translatable("peekinside.bundle"));
	}
}
