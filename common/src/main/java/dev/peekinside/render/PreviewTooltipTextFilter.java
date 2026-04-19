package dev.peekinside.render;

import dev.peekinside.config.PeekConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class PreviewTooltipTextFilter {
	private PreviewTooltipTextFilter() {
	}

	public static List<Component> filter(List<Component> lines) {
		if (!PeekConfig.INSTANCE.hideVanillaContainerLines) {
			return lines;
		}

		List<Component> filtered = new ArrayList<>(lines.size());
		boolean changed = false;
		for (Component line : lines) {
			if (isVanillaContainerLine(line)) {
				changed = true;
				continue;
			}

			filtered.add(line);
		}

		return changed ? List.copyOf(filtered) : lines;
	}

	private static boolean isVanillaContainerLine(Component line) {
		if (!(line.getContents() instanceof TranslatableContents translatable)) {
			return false;
		}

		String key = translatable.getKey();
		return "item.container.item_count".equals(key) || "item.container.more_items".equals(key);
	}
}
