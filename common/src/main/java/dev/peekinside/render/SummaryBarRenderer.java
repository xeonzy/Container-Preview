package dev.peekinside.render;

import dev.peekinside.config.PeekConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public final class SummaryBarRenderer {
	private SummaryBarRenderer() {
	}

	public static void render(GuiGraphics graphics, Font font, PreviewTooltipData data, PreviewPalette palette, int x, int y, int width) {
		PreviewAnalysis analysis = data.analysis();
		int contentY = y;
		if (data.result().label() != null) {
			graphics.drawString(font, data.result().label(), x, contentY, 0xFFFFFFFF, true);
			contentY += font.lineHeight + 2;
		}

		long nonEmpty = analysis != null ? analysis.nonEmptySlots() : countNonEmpty(data);
		long totalItems = analysis != null ? analysis.totalItems() : countItems(data);
		long totalSlots = data.result().slots().size();

		if (PeekConfig.INSTANCE.showSummaryBar) {
			String text = nonEmpty + "/" + totalSlots + " slots | " + totalItems + " items";
			graphics.drawString(font, text, x, contentY, palette.summaryTextColor(), false);

			int barY = contentY + font.lineHeight + 1;
			graphics.fill(x, barY, x + width, barY + 2, palette.slotBackgroundColor());

			float ratio = totalSlots > 0 ? (float) nonEmpty / totalSlots : 0.0F;
			int color = ratio > 0.5F ? 0xFF55FF55 : ratio > 0.2F ? 0xFFFFFF55 : 0xFFFF5555;
			int filledWidth = (int) (width * ratio);
			if (filledWidth > 0) {
				graphics.fill(x, barY, x + filledWidth, barY + 2, color);
			}
			contentY = barY + 3;
		}

		PreviewMarketData marketData = analysis != null ? analysis.marketData() : null;
		if (PeekConfig.INSTANCE.showSellerMetadata && marketData != null) {
			if (marketData.seller() != null) {
				graphics.drawString(
					font,
					Component.translatable("peekinside.market.seller", marketData.seller()),
					x,
					contentY,
					palette.summaryTextColor(),
					false
				);
				contentY += font.lineHeight + 1;
			}
			if (marketData.price() != null) {
				graphics.drawString(
					font,
					Component.translatable("peekinside.market.price", marketData.price()),
					x,
					contentY,
					palette.summaryTextColor(),
					false
				);
				contentY += font.lineHeight + 1;
			}
			if (marketData.quantity() != null) {
				graphics.drawString(
					font,
					Component.translatable("peekinside.market.quantity", marketData.quantity()),
					x,
					contentY,
					palette.summaryTextColor(),
					false
				);
				contentY += font.lineHeight + 1;
			}
		}

		if (PeekConfig.INSTANCE.showSearchBar && data.searchFilter().isActive()) {
			graphics.drawString(font, "> " + data.searchFilter().getQuery(), x, contentY, palette.searchTextColor(), false);
		}
	}

	public static int getHeight(Font font, PreviewTooltipData data) {
		int height = 0;
		if (data.result().label() != null) {
			height += font.lineHeight + 2;
		}

		if (PeekConfig.INSTANCE.showSummaryBar) {
			height += font.lineHeight + 3;
		}

		PreviewAnalysis analysis = data.analysis();
		PreviewMarketData marketData = analysis != null ? analysis.marketData() : null;
		if (PeekConfig.INSTANCE.showSellerMetadata && marketData != null) {
			if (marketData.seller() != null) {
				height += font.lineHeight + 1;
			}
			if (marketData.price() != null) {
				height += font.lineHeight + 1;
			}
			if (marketData.quantity() != null) {
				height += font.lineHeight + 1;
			}
		}

		if (PeekConfig.INSTANCE.showSearchBar && data.searchFilter().isActive()) {
			height += font.lineHeight + 1;
		}

		return height;
	}

	public static int getContentWidth(Font font, PreviewTooltipData data) {
		int width = 0;
		if (data.result().label() != null) {
			width = Math.max(width, font.width(data.result().label()));
		}
		if (PeekConfig.INSTANCE.showSummaryBar) {
			long nonEmpty = data.analysis() != null ? data.analysis().nonEmptySlots() : countNonEmpty(data);
			long totalItems = data.analysis() != null ? data.analysis().totalItems() : countItems(data);
			String text = nonEmpty + "/" + data.result().slots().size() + " slots | " + totalItems + " items";
			width = Math.max(width, font.width(text));
		}
		PreviewAnalysis analysis = data.analysis();
		PreviewMarketData marketData = analysis != null ? analysis.marketData() : null;
		if (PeekConfig.INSTANCE.showSellerMetadata && marketData != null) {
			if (marketData.seller() != null) {
				width = Math.max(width, font.width(Component.translatable("peekinside.market.seller", marketData.seller())));
			}
			if (marketData.price() != null) {
				width = Math.max(width, font.width(Component.translatable("peekinside.market.price", marketData.price())));
			}
			if (marketData.quantity() != null) {
				width = Math.max(width, font.width(Component.translatable("peekinside.market.quantity", marketData.quantity())));
			}
		}
		if (PeekConfig.INSTANCE.showSearchBar && data.searchFilter().isActive()) {
			width = Math.max(width, font.width("> " + data.searchFilter().getQuery()));
		}
		return width;
	}

	private static long countNonEmpty(PreviewTooltipData data) {
		long count = 0L;
		for (ItemStack stack : data.result().slots()) {
			if (!stack.isEmpty()) {
				count++;
			}
		}
		return count;
	}

	private static long countItems(PreviewTooltipData data) {
		long count = 0L;
		for (ItemStack stack : data.result().slots()) {
			if (!stack.isEmpty()) {
				count += stack.getCount();
			}
		}
		return count;
	}
}
