package dev.peekinside.render;

import dev.peekinside.api.PreviewResult;
import dev.peekinside.config.PeekConfig;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class PreviewCache {
	public static final PreviewCache INSTANCE = new PreviewCache();

	private final Map<Long, PreviewResult> entries = new LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, PreviewResult> eldest) {
			return this.size() > PeekConfig.INSTANCE.cacheSize;
		}
	};

	private PreviewCache() {
	}

	public @Nullable PreviewResult getOrCompute(ItemStack stack, Supplier<@Nullable PreviewResult> supplier) {
		long cacheKey = cacheKey(stack);
		PreviewResult cached = this.entries.get(cacheKey);
		if (cached != null) {
			return cached;
		}

		PreviewResult computed = supplier.get();
		if (computed != null) {
			this.entries.put(cacheKey, computed);
		}

		return computed;
	}

	public void clear() {
		this.entries.clear();
	}

	private static long cacheKey(ItemStack stack) {
		return ((long) System.identityHashCode(stack.getItem()) << 32)
			| (stack.getComponents().hashCode() & 0xFFFFFFFFL);
	}
}
