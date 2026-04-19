package dev.peekinside;

import java.text.DecimalFormat;

public final class PeekItemCountFormat {
	private static final DecimalFormat COMPACT_FORMAT = new DecimalFormat("#.#");

	private PeekItemCountFormat() {
	}

	public static String format(int count) {
		return format((long) count);
	}

	public static String format(long count) {
		if (count < 1000) {
			return String.valueOf(count);
		}

		if (count < 1000000) {
			return COMPACT_FORMAT.format(count / 1000.0) + "k";
		}

		return COMPACT_FORMAT.format(count / 1000000.0) + "m";
	}
}
