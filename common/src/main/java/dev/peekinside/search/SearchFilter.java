package dev.peekinside.search;

import java.util.Locale;
import net.minecraft.world.item.ItemStack;

public final class SearchFilter {
	public static final SearchFilter INSTANCE = new SearchFilter();

	private String query = "";

	private SearchFilter() {
	}

	public boolean isActive() {
		return !this.query.isEmpty();
	}

	public String getQuery() {
		return this.query;
	}

	public boolean matches(ItemStack stack) {
		if (!this.isActive() || stack.isEmpty()) {
			return true;
		}

		String loweredQuery = this.query.toLowerCase(Locale.ROOT);
		String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
		return name.contains(loweredQuery);
	}

	public void append(char character) {
		this.query += character;
	}

	public void backspace() {
		if (!this.query.isEmpty()) {
			this.query = this.query.substring(0, this.query.length() - 1);
		}
	}

	public void clear() {
		this.query = "";
	}
}
