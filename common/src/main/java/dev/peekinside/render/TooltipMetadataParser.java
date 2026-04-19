package dev.peekinside.render;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public final class TooltipMetadataParser {
	private static final Pattern SELLER_PATTERN = Pattern.compile("(?i)\\b(?:seller|owner|listed by|vendor)\\b\\s*[:>-]?\\s*(.+)");
	private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)\\b(?:price|cost|buy(?: price)?|sell(?: price)?|bid|starting bid)\\b\\s*[:>-]?\\s*([^\\s].+)");
	private static final Pattern QUANTITY_PATTERN = Pattern.compile("(?i)\\b(?:qty|quantity|amount|count|stack size)\\b\\s*[:>-]?\\s*([^\\s].+)");
	private static final Pattern CURRENCY_FALLBACK = Pattern.compile("([$€£]\\s*[0-9][0-9,._]*(?:\\s*[a-zA-Z]+)?|[0-9][0-9,._]*\\s*(?:coins?|credits?|tokens?))");

	private TooltipMetadataParser() {
	}

	public static @Nullable PreviewMarketData parse(List<String> rawLines) {
		String seller = null;
		String price = null;
		String quantity = null;

		for (String rawLine : rawLines) {
			String line = normalize(rawLine);
			if (line.isBlank()) {
				continue;
			}

			if (seller == null) {
				seller = matchValue(SELLER_PATTERN, line);
			}
			if (price == null) {
				price = matchValue(PRICE_PATTERN, line);
			}
			if (quantity == null) {
				quantity = matchValue(QUANTITY_PATTERN, line);
			}
		}

		if (price == null) {
			for (String rawLine : rawLines) {
				Matcher matcher = CURRENCY_FALLBACK.matcher(normalize(rawLine));
				if (matcher.find()) {
					price = matcher.group(1).trim();
					break;
				}
			}
		}

		PreviewMarketData data = new PreviewMarketData(cleanValue(seller), cleanValue(price), cleanValue(quantity));
		return data.isEmpty() ? null : data;
	}

	private static @Nullable String matchValue(Pattern pattern, String line) {
		Matcher matcher = pattern.matcher(line);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	private static String normalize(String input) {
		return input.replace('\u00A0', ' ').trim();
	}

	private static @Nullable String cleanValue(@Nullable String value) {
		if (value == null) {
			return null;
		}

		String cleaned = value.trim();
		if (cleaned.isEmpty()) {
			return null;
		}

		String lowered = cleaned.toLowerCase(Locale.ROOT);
		return switch (lowered) {
			case "-", "n/a", "none", "unknown" -> null;
			default -> cleaned;
		};
	}
}
