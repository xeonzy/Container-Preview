package dev.peekinside.render;

import dev.peekinside.api.PreviewResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public record PreviewSnapshot(
	ItemStack sourceStack,
	PreviewResult result,
	List<String> tooltipLines
) {
	public static PreviewSnapshot capture(ItemStack sourceStack, PreviewResult result, List<Component> tooltipLines) {
		List<ItemStack> copiedSlots = new ArrayList<>(result.slots().size());
		for (ItemStack stack : result.slots()) {
			copiedSlots.add(stack.copy());
		}

		List<String> copiedTooltipLines = new ArrayList<>(tooltipLines.size());
		for (Component line : tooltipLines) {
			copiedTooltipLines.add(line.getString());
		}

		return new PreviewSnapshot(
			sourceStack.copy(),
			new PreviewResult(List.copyOf(copiedSlots), result.columns(), result.rows(), result.label()),
			List.copyOf(copiedTooltipLines)
		);
	}
}
