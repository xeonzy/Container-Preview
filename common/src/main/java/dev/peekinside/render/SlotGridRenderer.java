package dev.peekinside.render;

import dev.peekinside.search.SearchFilter;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public final class SlotGridRenderer {
	private SlotGridRenderer() {
	}

	public static void render(
		GuiGraphics graphics,
		Font font,
		List<ItemStack> slots,
		int columns,
		int rows,
		int rowOffset,
		int visibleRows,
		int startX,
		int startY,
		SearchFilter filter,
		PreviewPalette palette,
		int hoveredSlotIndex
	) {
		int slotSize = PreviewTooltipComponent.slotSize();
		int iconInset = PreviewTooltipComponent.iconInset();
		for (int visibleIndex = 0; visibleIndex < columns * visibleRows; visibleIndex++) {
			int row = visibleIndex / columns;
			int column = visibleIndex % columns;
			int index = (rowOffset + row) * columns + column;
			int x = startX + column * slotSize;
			int y = startY + row * slotSize;

			graphics.fill(x, y, x + slotSize, y + slotSize, palette.slotBackgroundColor());

			ItemStack stack = index < slots.size() ? slots.get(index) : ItemStack.EMPTY;
			if (!stack.isEmpty()) {
				graphics.renderItem(stack, x + iconInset, y + iconInset);
				if (dev.peekinside.config.PeekConfig.INSTANCE.showSlotItemCounts) {
					String countText = null;
					if (dev.peekinside.config.PeekConfig.INSTANCE.compactNumberFormatting && stack.getCount() > 1) {
						countText = dev.peekinside.PeekItemCountFormat.format(stack.getCount());
					}
					graphics.renderItemDecorations(font, stack, x + iconInset, y + iconInset, countText);
				}

				if (filter.isActive() && !filter.matches(stack)) {
					graphics.fill(x, y, x + slotSize, y + slotSize, 0xAA000000);
				}
			}

			if (index == hoveredSlotIndex) {
				graphics.fill(x, y, x + slotSize, y + slotSize, palette.hoveredSlotColor());
			}

			drawCellFrame(graphics, x, y, slotSize, palette.borderColor());
		}
	}

	private static void drawCellFrame(GuiGraphics graphics, int x, int y, int size, int borderColor) {
		int subtle = borderColor & 0x20FFFFFF;
		int edge = borderColor & 0x14FFFFFF;
		graphics.fill(x, y, x + size, y + 1, subtle);
		graphics.fill(x, y, x + 1, y + size, subtle);
		graphics.fill(x, y + size - 1, x + size, y + size, edge);
		graphics.fill(x + size - 1, y, x + size, y + size, edge);
	}
}
