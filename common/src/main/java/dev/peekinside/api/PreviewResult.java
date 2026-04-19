package dev.peekinside.api;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * slots.size() must equal columns * rows. Use ItemStack.EMPTY for gaps.
 */
public record PreviewResult(
	List<ItemStack> slots,
	int columns,
	int rows,
	@Nullable Component label
) {
}
