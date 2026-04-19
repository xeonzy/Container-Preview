package dev.peekinside.provider;

import dev.peekinside.api.ContainerProvider;
import dev.peekinside.api.PreviewRequest;
import dev.peekinside.api.PreviewResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("null")
public final class ComponentContainerProvider implements ContainerProvider {
	public static final int PRIORITY = 1000;

	@Override
	public @Nullable PreviewResult provide(PreviewRequest request) {
		// First try vanilla container
		ItemContainerContents contents = request.stack().get(DataComponents.CONTAINER);
		if (contents != null) {
			return fromItemContainerContents(request, contents);
		}

		// Then scan all components for anything that looks like a list of items (even inside records!)
		for (var entry : request.stack().getComponents()) {
			Object value = entry.value();
			if (value == null) continue;

			PreviewResult result = tryExtractFromObject(request, value);
			if (result != null) {
				return result;
			}
		}

		return null;
	}

	private @Nullable PreviewResult tryExtractFromObject(PreviewRequest request, Object value) {
		if (value instanceof ItemContainerContents icc) {
			return fromItemContainerContents(request, icc);
		}
		if (value instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof ItemStack) {
			return fromItemList(request, (List<ItemStack>) list);
		}

		// Deep inspection for records/objects containing item lists
		try {
			for (java.lang.reflect.Field field : value.getClass().getDeclaredFields()) {
				field.setAccessible(true);
				Object fieldValue = field.get(value);
				if (fieldValue instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof ItemStack) {
					return fromItemList(request, (List<ItemStack>) list);
				}
				if (fieldValue instanceof ItemContainerContents icc) {
					return fromItemContainerContents(request, icc);
				}
			}
		} catch (Exception ignored) {
		}

		return null;
	}

	private PreviewResult fromItemContainerContents(PreviewRequest request, ItemContainerContents contents) {
		GridShapeRegistry shapes = GridShapeRegistry.INSTANCE;
		int count = (int) contents.stream().count();
		int cols = shapes.getCols(request.stack().getItem(), count);
		int rows = shapes.getRows(request.stack().getItem(), count, cols);

		NonNullList<ItemStack> copiedSlots = NonNullList.withSize(cols * rows, ItemStack.EMPTY);
		contents.copyInto(copiedSlots);

		return new PreviewResult(new ArrayList<>(copiedSlots), cols, rows, null);
	}

	private PreviewResult fromItemList(PreviewRequest request, List<ItemStack> items) {
		GridShapeRegistry shapes = GridShapeRegistry.INSTANCE;
		int cols = shapes.getCols(request.stack().getItem(), items.size());
		int rows = shapes.getRows(request.stack().getItem(), items.size(), cols);

		List<ItemStack> copied = new ArrayList<>(cols * rows);
		for (int i = 0; i < cols * rows; i++) {
			copied.add(i < items.size() ? items.get(i).copy() : ItemStack.EMPTY);
		}

		return new PreviewResult(copied, cols, rows, null);
	}
}
