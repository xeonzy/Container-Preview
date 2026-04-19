package dev.peekinside.render;

import dev.peekinside.api.PreviewResult;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class PreviewAnalyzer {
	private PreviewAnalyzer() {
	}

	public static PreviewAnalysis analyze(PreviewSnapshot snapshot) {
		PreviewResult result = snapshot.result();
		long nonEmptySlots = 0L;
		long totalItems = 0L;
		for (ItemStack stack : result.slots()) {
			if (!stack.isEmpty()) {
				nonEmptySlots++;
				totalItems += stack.getCount();
			}
		}

		CompressedPreview compressed = buildCompressed(result);
		PreviewMarketData marketData = TooltipMetadataParser.parse(snapshot.tooltipLines());
		if (marketData != null && marketData.quantity() == null && snapshot.sourceStack().getCount() > 1) {
			marketData = new PreviewMarketData(marketData.seller(), marketData.price(), Integer.toString(snapshot.sourceStack().getCount()));
		}

		return new PreviewAnalysis(nonEmptySlots, totalItems, compressed, marketData);
	}

	private static @Nullable CompressedPreview buildCompressed(PreviewResult result) {
		ItemStack representative = ItemStack.EMPTY;
		long totalCount = 0L;
		int stackCount = 0;
		for (ItemStack stack : result.slots()) {
			if (stack.isEmpty()) {
				continue;
			}

			if (representative.isEmpty()) {
				representative = stack.copy();
			} else if (!ItemStack.isSameItemSameComponents(representative, stack)) {
				return null;
			}

			totalCount += stack.getCount();
			stackCount++;
		}

		if (stackCount <= 1 || representative.isEmpty()) {
			return null;
		}

		return new CompressedPreview(representative, totalCount, stackCount);
	}
}
