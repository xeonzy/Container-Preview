package dev.peekinside.render;

import dev.peekinside.config.PeekConfig;
import org.jspecify.annotations.Nullable;

public record PreviewAnalysis(
	long nonEmptySlots,
	long totalItems,
	@Nullable CompressedPreview compressed,
	@Nullable PreviewMarketData marketData
) {
	public boolean useCompressedView(boolean detailMode) {
		return PeekConfig.INSTANCE.compressionEnabled && !detailMode && this.compressed != null;
	}
}
