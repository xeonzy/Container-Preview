package dev.peekinside.render;

import dev.peekinside.api.PreviewResult;
import dev.peekinside.config.PeekConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public final class PreviewTooltipComponent implements ClientTooltipComponent {
	public static final int DEFAULT_SLOT_SIZE = 18;
	public static final int DEFAULT_PADDING = 4;

	private final PreviewTooltipData data;

	public PreviewTooltipComponent(PreviewTooltipData data) {
		this.data = data;
	}

	@Override
	public int getHeight(Font font) {
		return this.getPanelHeight(font, this.data);
	}

	@Override
	public int getWidth(Font font) {
		return this.getPanelWidth(font, this.data);
	}

	@Override
	public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics graphics) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null) {
			return;
		}

		Screen screen = minecraft.screen;
		if (screen == null) {
			return;
		}

		double mouseX = minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
		double mouseY = minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
		PreviewPalette palette = this.data.palette();
		int panelWidth = this.getPanelWidth(font, this.data);
		int panelHeight = this.getPanelHeight(font, this.data);
		PanelPosition panelPosition = fitPanelPosition(x, x, y, panelWidth, panelHeight, graphics.guiWidth(), graphics.guiHeight());
		int panelX = panelPosition.x();
		int panelY = panelPosition.y();
		long signature = PreviewAnalysisCache.signature(this.data.sourceStack(), this.data.result(), this.data.tooltipLines())
			^ (this.data.detailMode() ? 0x9E3779B97F4A7C15L : 0L);

		graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, palette.panelBackgroundColor());
		graphics.renderOutline(panelX, panelY, panelWidth, panelHeight, palette.borderColor());
		if (this.isEmptyView(this.data) && !this.data.detailMode()) {
			this.renderEmptyPanel(graphics, font, panelX, panelY, panelWidth, panelHeight, this.data);
			return;
		}

		if (this.useCompressedView()) {
			this.renderCompressedPanel(graphics, font, screen, this.data, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);
			return;
		}

		PreviewResult result = this.data.result();
		int visibleRows = this.visibleRows(result);
		int rowOffset = Math.min(PreviewState.scrollOffset(), Math.max(0, result.rows() - visibleRows));
		PreviewState.registerPanelBounds(screen, signature, panelX, panelY, panelWidth, panelHeight, result.rows(), visibleRows);

		int innerWidth = panelWidth - padding() * 2;
		int summaryX = panelX + padding();
		int summaryY = panelY + padding();
		int summaryHeight = SummaryBarRenderer.getHeight(font, this.data);
		int bodyY = summaryY + summaryHeight;
		SummaryBarRenderer.render(graphics, font, this.data, palette, summaryX, summaryY, innerWidth);

		int gridX = panelX + padding();
		int hoveredSlotIndex = hitTestGrid(result, gridX, bodyY, mouseX, mouseY, rowOffset, visibleRows);

		NestedPreviewState.INSTANCE.clear();
		NestedPreviewState.INSTANCE.setHoveredSlotIndex(hoveredSlotIndex);
		SlotGridRenderer.render(
			graphics,
			font,
			result.slots(),
			result.columns(),
			result.rows(),
			rowOffset,
			visibleRows,
			gridX,
			bodyY,
			this.data.searchFilter(),
			palette,
			hoveredSlotIndex
		);

		if (result.rows() > visibleRows) {
			this.renderScrollbar(graphics, panelX + panelWidth - padding() - 2, bodyY, visibleRows, result.rows(), rowOffset, palette);
		}

		this.renderHoveredStackTooltip(minecraft, graphics, font, mouseX, mouseY, this.stackAt(result, hoveredSlotIndex));
		if (PeekConfig.INSTANCE.nestedPreviewEnabled && PeekConfig.INSTANCE.maxNestedDepth > 0) {
			this.renderNestedPanels(
				graphics,
				font,
				screen,
				result,
				panelX,
				panelY,
				panelWidth,
				gridX,
				bodyY,
				mouseX,
				mouseY,
				rowOffset,
				visibleRows,
				0
			);
		}
	}

	public static int slotSize() {
		return Math.max(16, Math.round(PeekConfig.INSTANCE.slotSize * (PeekConfig.INSTANCE.uiScale / 100.0F)));
	}

	public static int padding() {
		return PeekConfig.INSTANCE.padding;
	}

	public static int iconInset() {
		return Math.max(0, (slotSize() - 16) / 2);
	}

	private void renderCompressedPanel(
		GuiGraphics graphics,
		Font font,
		Screen screen,
		PreviewTooltipData data,
		int panelX,
		int panelY,
		int panelWidth,
		int panelHeight,
		double mouseX,
		double mouseY
	) {
		if (data.analysis() == null || data.analysis().compressed() == null) {
			return;
		}

		CompressedPreview compressed = data.analysis().compressed();
		ItemStack stack = compressed.stack();
		int size = slotSize();
		int slotX = panelX + padding();
		int slotY = panelY + padding();
		boolean hovered = mouseX >= slotX && mouseY >= slotY && mouseX < slotX + size && mouseY < slotY + size;

		graphics.fill(slotX, slotY, slotX + size, slotY + size, data.palette().slotBackgroundColor());
		if (hovered) {
			graphics.fill(slotX, slotY, slotX + size, slotY + size, data.palette().hoveredSlotColor());
		}
		drawCellFrame(graphics, slotX, slotY, size, data.palette().borderColor());

		int itemX = slotX + iconInset();
		int itemY = slotY + iconInset();
		graphics.renderItem(stack, itemX, itemY);
		// Don't render default decorations (clusters numbers)
		
		// Render count separately for better polish
		String countText = PeekConfig.INSTANCE.compactNumberFormatting 
			? "x" + dev.peekinside.PeekItemCountFormat.format(compressed.totalCount())
			: String.format("x%,d", compressed.totalCount());
		int textX = slotX + size + padding();
		int textY = slotY + (size - font.lineHeight) / 2 + 1;
		graphics.drawString(font, countText, textX, textY, 0xFFFFFFFF, true);

		if (hovered) {
			this.renderHoveredStackTooltip(Minecraft.getInstance(), graphics, font, mouseX, mouseY, stack);
			if (PeekConfig.INSTANCE.nestedPreviewEnabled && PeekConfig.INSTANCE.maxNestedDepth > 0) {
				PreviewResult nested = PreviewState.resolvePreview(stack, Minecraft.getInstance().player, screen, data.detailMode());
				if (nested != null) {
			this.renderNestedPanel(graphics, font, screen, stack, nested, panelX, panelX + panelWidth + padding(), panelY, mouseX, mouseY, 0, data.detailMode());
				}
			}
		}
	}

	private void renderNestedPanels(
		GuiGraphics graphics,
		Font font,
		Screen screen,
		PreviewResult parent,
		int panelX,
		int panelY,
		int panelWidth,
		int gridX,
		int gridY,
		double mouseX,
		double mouseY,
		int rowOffset,
		int visibleRows,
		int depth
	) {
		if (depth >= PeekConfig.INSTANCE.maxNestedDepth) {
			return;
		}

		int hoveredSlotIndex = hitTestGrid(parent, gridX, gridY, mouseX, mouseY, rowOffset, visibleRows);
		ItemStack hoveredStack = this.stackAt(parent, hoveredSlotIndex);
		if (hoveredStack.isEmpty()) {
			return;
		}

		PreviewResult nested = PreviewState.resolvePreview(hoveredStack, Minecraft.getInstance().player, screen, this.data.detailMode());
		if (nested == null) {
			return;
		}

		NestedPreviewState.INSTANCE.push(nested);
		int nestedX = panelX + panelWidth + padding();
		this.renderNestedPanel(graphics, font, screen, hoveredStack, nested, panelX, nestedX, panelY, mouseX, mouseY, depth, this.data.detailMode());
	}

	private void renderNestedPanel(
		GuiGraphics graphics,
		Font font,
		Screen screen,
		ItemStack sourceStack,
		PreviewResult result,
		int sourceX,
		int preferredX,
		int preferredY,
		double mouseX,
		double mouseY,
		int depth,
		boolean detailMode
	) {
		List<Component> tooltipLines = Screen.getTooltipFromItem(Minecraft.getInstance(), sourceStack);
		PreviewAnalysis analysis = PreviewAnalysisCache.INSTANCE.getOrSchedule(sourceStack, result, tooltipLines);
		PreviewTooltipData nestedData = new PreviewTooltipData(
			sourceStack.copy(),
			result,
			this.data.searchFilter(),
			PreviewPaletteResolver.resolve(sourceStack),
			List.copyOf(tooltipLines),
			detailMode,
			analysis
		);
		if (this.isEmptyView(nestedData)) {
			int panelWidth = this.getPanelWidth(font, nestedData);
			int panelHeight = this.getPanelHeight(font, nestedData);
			PanelPosition position = fitPanelPosition(sourceX, preferredX, preferredY, panelWidth, panelHeight, graphics.guiWidth(), graphics.guiHeight());
			int x = position.x();
			int y = position.y();
			long signature = PreviewAnalysisCache.signature(nestedData.sourceStack(), nestedData.result(), nestedData.tooltipLines())
				^ (nestedData.detailMode() ? 0x9E3779B97F4A7C15L : 0L);
			PreviewState.registerPanelBounds(screen, signature, x, y, panelWidth, panelHeight, 1, 1);
			graphics.fill(x, y, x + panelWidth, y + panelHeight, nestedData.palette().panelBackgroundColor());
			graphics.renderOutline(x, y, panelWidth, panelHeight, nestedData.palette().borderColor());
			this.renderEmptyPanel(graphics, font, x, y, panelWidth, panelHeight, nestedData);
			return;
		}
		if (this.useCompressedView(nestedData)) {
			int panelWidth = this.getPanelWidth(font, nestedData);
			int panelHeight = this.getPanelHeight(font, nestedData);
			PanelPosition position = fitPanelPosition(sourceX, preferredX, preferredY, panelWidth, panelHeight, graphics.guiWidth(), graphics.guiHeight());
			int x = position.x();
			int y = position.y();
			long signature = PreviewAnalysisCache.signature(nestedData.sourceStack(), nestedData.result(), nestedData.tooltipLines())
				^ (nestedData.detailMode() ? 0x9E3779B97F4A7C15L : 0L);
			PreviewState.registerPanelBounds(screen, signature, x, y, panelWidth, panelHeight, 1, 1);
			graphics.fill(x, y, x + panelWidth, y + panelHeight, nestedData.palette().panelBackgroundColor());
			graphics.renderOutline(x, y, panelWidth, panelHeight, nestedData.palette().borderColor());
			this.renderCompressedPanel(graphics, font, screen, nestedData, x, y, panelWidth, panelHeight, mouseX, mouseY);
			return;
		}

		int labelHeight = result.label() == null ? 0 : font.lineHeight + 2;
		int visibleRows = this.visibleRows(result);
		int gridWidth = result.columns() * slotSize();
		int panelWidth = Math.max(gridWidth, result.label() == null ? 0 : font.width(result.label())) + padding() * 2;
		int panelHeight = labelHeight + visibleRows * slotSize() + padding() * 2;
		PanelPosition position = fitPanelPosition(sourceX, preferredX, preferredY, panelWidth, panelHeight, graphics.guiWidth(), graphics.guiHeight());
		int x = position.x();
		int y = position.y();
		int gridX = x + padding();
		int gridY = y + padding() + labelHeight;
		PreviewPalette nestedPalette = PreviewPaletteResolver.resolve(sourceStack);

		graphics.fill(x, y, x + panelWidth, y + panelHeight, nestedPalette.panelBackgroundColor());
		graphics.renderOutline(x, y, panelWidth, panelHeight, nestedPalette.borderColor());
		if (result.label() != null) {
			graphics.drawString(font, result.label(), x + padding(), y + padding(), 0xFFFFFFFF, true);
		}

		int hoveredSlotIndex = hitTestGrid(result, gridX, gridY, mouseX, mouseY, 0, visibleRows);
		SlotGridRenderer.render(
			graphics,
			font,
			result.slots(),
			result.columns(),
			result.rows(),
			0,
			visibleRows,
			gridX,
			gridY,
			this.data.searchFilter(),
			nestedPalette,
			hoveredSlotIndex
		);

		ItemStack hoveredStack = this.stackAt(result, hoveredSlotIndex);
		if (!hoveredStack.isEmpty()) {
			this.renderHoveredStackTooltip(Minecraft.getInstance(), graphics, font, mouseX, mouseY, hoveredStack);
			if (depth + 1 < PeekConfig.INSTANCE.maxNestedDepth) {
				PreviewResult nested = PreviewState.resolvePreview(hoveredStack, Minecraft.getInstance().player, screen, this.data.detailMode());
				if (nested != null) {
					this.renderNestedPanel(graphics, font, screen, hoveredStack, nested, x, x + panelWidth + padding(), y, mouseX, mouseY, depth + 1, detailMode);
				}
			}
		}
	}

	private void renderHoveredStackTooltip(Minecraft minecraft, GuiGraphics graphics, Font font, double mouseX, double mouseY, ItemStack stack) {
		if (stack.isEmpty()) {
			return;
		}

		List<Component> tooltipLines = PreviewTooltipTextFilter.filter(Screen.getTooltipFromItem(minecraft, stack));
		List<ClientTooltipComponent> tooltipComponents = new ArrayList<>(tooltipLines.size() + 1);
		for (Component line : tooltipLines) {
			tooltipComponents.add(ClientTooltipComponent.create(line.getVisualOrderText()));
		}
		stack.getTooltipImage().ifPresent(image -> tooltipComponents.add(ClientTooltipComponent.create(image)));

		graphics.renderTooltip(
			font,
			tooltipComponents,
			(int) mouseX,
			(int) mouseY,
			DefaultTooltipPositioner.INSTANCE,
			stack.get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE)
		);
	}

	private void renderScrollbar(
		GuiGraphics graphics,
		int x,
		int y,
		int visibleRows,
		int totalRows,
		int rowOffset,
		PreviewPalette palette
	) {
		int height = visibleRows * slotSize();
		graphics.fill(x, y, x + 2, y + height, palette.slotBackgroundColor());
		int thumbHeight = Math.max(8, Math.round(height * (visibleRows / (float) totalRows)));
		int travel = Math.max(0, height - thumbHeight);
		int thumbY = y;
		if (travel > 0 && totalRows > visibleRows) {
			thumbY += Math.round(travel * (rowOffset / (float) (totalRows - visibleRows)));
		}
		graphics.fill(x, thumbY, x + 2, thumbY + thumbHeight, palette.summaryTextColor());
	}

	private ItemStack stackAt(PreviewResult result, int slotIndex) {
		return slotIndex >= 0 && slotIndex < result.slots().size() ? result.slots().get(slotIndex) : ItemStack.EMPTY;
	}

	private int visibleRows(PreviewResult result) {
		return Math.min(result.rows(), PeekConfig.INSTANCE.maxVisibleRows);
	}

	private boolean useCompressedView() {
		return this.useCompressedView(this.data);
	}

	private boolean useCompressedView(PreviewTooltipData data) {
		return data.analysis() != null && data.analysis().useCompressedView(data.detailMode());
	}

	private int getPanelWidth(Font font, PreviewTooltipData data) {
		if (this.isEmptyView(data) && !data.detailMode()) {
			String emptyText = this.emptyText();
			if (emptyText.isBlank()) {
				return slotSize() + padding() * 2;
			}
			return slotSize() + padding() * 3 + font.width(emptyText);
		}

		if (this.useCompressedView(data)) {
			CompressedPreview compressed = data.analysis().compressed();
			String countText = PeekConfig.INSTANCE.compactNumberFormatting 
				? "x" + dev.peekinside.PeekItemCountFormat.format(compressed.totalCount())
				: String.format("x%,d", compressed.totalCount());
			return slotSize() + padding() * 3 + font.width(countText);
		}

		return Math.max(data.result().columns() * slotSize(), SummaryBarRenderer.getContentWidth(font, data)) + padding() * 2;
	}

	private int getPanelHeight(Font font, PreviewTooltipData data) {
		if (this.isEmptyView(data) && !data.detailMode()) {
			return slotSize() + padding() * 2;
		}

		if (this.useCompressedView(data)) {
			return slotSize() + padding() * 2;
		}

		return SummaryBarRenderer.getHeight(font, data) + this.visibleRows(data.result()) * slotSize() + padding() * 2;
	}

	private static int fitPanelX(int anchorX, int preferredX, int panelWidth, int screenWidth) {
		if (panelWidth >= screenWidth) {
			return 0;
		}

		int rightX = preferredX;
		if (rightX >= 0 && rightX + panelWidth <= screenWidth) {
			return rightX;
		}

		int leftX = anchorX - panelWidth;
		if (leftX >= 0 && leftX + panelWidth <= screenWidth) {
			return leftX;
		}

		return clamp(preferredX, 0, screenWidth - panelWidth);
	}

	private static int fitPanelY(int preferredY, int panelHeight, int screenHeight) {
		if (panelHeight >= screenHeight) {
			return 0;
		}

		return clamp(preferredY, 0, screenHeight - panelHeight);
	}

	private static PanelPosition fitPanelPosition(int anchorX, int preferredX, int preferredY, int panelWidth, int panelHeight, int screenWidth, int screenHeight) {
		return new PanelPosition(
			fitPanelX(anchorX, preferredX, panelWidth, screenWidth),
			fitPanelY(preferredY, panelHeight, screenHeight)
		);
	}

	private boolean isEmptyView(PreviewTooltipData data) {
		return data.analysis() != null && data.analysis().nonEmptySlots() == 0L;
	}

	private String emptyText() {
		return PeekConfig.INSTANCE.showEmptyContainerMessage ? net.minecraft.network.chat.Component.translatable("peekinside.empty_container").getString() : "";
	}

	private void renderEmptyPanel(
		GuiGraphics graphics,
		Font font,
		int panelX,
		int panelY,
		int panelWidth,
		int panelHeight,
		PreviewTooltipData data
	) {
		int size = slotSize();
		int slotX = panelX + padding();
		int slotY = panelY + padding();
		graphics.fill(slotX, slotY, slotX + size, slotY + size, data.palette().slotBackgroundColor());
		drawCellFrame(graphics, slotX, slotY, size, data.palette().borderColor());
		String emptyText = this.emptyText();
		if (emptyText.isBlank()) {
			return;
		}

		int textX = slotX + size + padding();
		int textY = slotY + Math.max(0, (size - font.lineHeight) / 2);
		graphics.drawString(font, emptyText, textX, textY, 0xFFFFAA55, false);
	}

	private void drawCellFrame(GuiGraphics graphics, int x, int y, int size, int borderColor) {
		int topLeft = borderColor & 0x26FFFFFF;
		int bottomRight = borderColor & 0x10FFFFFF;
		graphics.fill(x, y, x + size, y + 1, topLeft);
		graphics.fill(x, y, x + 1, y + size, topLeft);
		graphics.fill(x, y + size - 1, x + size, y + size, bottomRight);
		graphics.fill(x + size - 1, y, x + size, y + size, bottomRight);
	}

	private static int hitTestGrid(
		PreviewResult result,
		int gridX,
		int gridY,
		double mouseX,
		double mouseY,
		int rowOffset,
		int visibleRows
	) {
		int relativeX = (int) mouseX - gridX;
		int relativeY = (int) mouseY - gridY;
		int width = result.columns() * slotSize();
		int height = visibleRows * slotSize();
		if (relativeX < 0 || relativeY < 0 || relativeX >= width || relativeY >= height) {
			return -1;
		}

		int column = relativeX / slotSize();
		int row = relativeY / slotSize();
		int slotIndex = (rowOffset + row) * result.columns() + column;
		return slotIndex < result.slots().size() ? slotIndex : -1;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private record PanelPosition(int x, int y) {
	}
}
