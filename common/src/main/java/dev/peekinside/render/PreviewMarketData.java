package dev.peekinside.render;

import org.jspecify.annotations.Nullable;

public record PreviewMarketData(
	@Nullable String seller,
	@Nullable String price,
	@Nullable String quantity
) {
	public boolean isEmpty() {
		return this.seller == null && this.price == null && this.quantity == null;
	}
}
