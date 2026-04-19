package dev.peekinside.provider;

import dev.peekinside.api.ContainerProvider;
import dev.peekinside.api.PreviewRequest;
import dev.peekinside.api.PreviewResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"unchecked", "null"})
public final class BlankPackProvider implements ContainerProvider {
	public static final int PRIORITY = 900;
	private static final Identifier BACKPACK_ITEMS_COMPONENT_ID = Identifier.fromNamespaceAndPath("yyzsbackpack", "backpack_items");

	@Override
	public @Nullable PreviewResult provide(PreviewRequest request) {
		DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(BACKPACK_ITEMS_COMPONENT_ID).orElse(null);
		if (componentType == null) {
			return null;
		}

		Object rawValue = request.stack().get((DataComponentType<Object>) componentType);
		if (!(rawValue instanceof List<?> rawSlots)) {
			return null;
		}

		List<ItemStack> slots = new ArrayList<>(rawSlots.size());
		int nonEmptySlots = 0;
		for (Object rawSlot : rawSlots) {
			if (rawSlot instanceof ItemStack stack) {
				ItemStack copy = stack.copy();
				slots.add(copy);
				if (!copy.isEmpty()) {
					nonEmptySlots++;
				}
			} else {
				slots.add(ItemStack.EMPTY);
			}
		}

		if (nonEmptySlots == 0) {
			return null;
		}

		int cols = GridShapeRegistry.INSTANCE.getCols(request.stack().getItem(), slots.size());
		int rows = GridShapeRegistry.INSTANCE.getRows(request.stack().getItem(), slots.size(), cols);
		int requiredSize = cols * rows;
		while (slots.size() < requiredSize) {
			slots.add(ItemStack.EMPTY);
		}

		return new PreviewResult(List.copyOf(slots), cols, rows, null);
	}
}
