package dev.peekinside.render;

import dev.peekinside.PeekConstants;

import dev.peekinside.PeekInside;
import dev.peekinside.api.PreviewResult;
import dev.peekinside.config.PeekConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("null")
public final class PreviewAnalysisCache {
	public static final PreviewAnalysisCache INSTANCE = new PreviewAnalysisCache();

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "peekinside-preview-analysis");
		thread.setDaemon(true);
		return thread;
	});

	private final Map<Long, PreviewAnalysis> completed = new LinkedHashMap<>(16, 0.75F, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<Long, PreviewAnalysis> eldest) {
			return this.size() > PeekConfig.INSTANCE.cacheSize;
		}
	};
	private final Map<Long, CompletableFuture<PreviewAnalysis>> pending = new ConcurrentHashMap<>();

	private PreviewAnalysisCache() {
	}

	public @Nullable PreviewAnalysis getOrSchedule(ItemStack sourceStack, PreviewResult result, List<Component> tooltipLines) {
		long signature = signature(sourceStack, result, tooltipLines);
		synchronized (this.completed) {
			PreviewAnalysis cached = this.completed.get(signature);
			if (cached != null) {
				return cached;
			}
		}

		CompletableFuture<PreviewAnalysis> future = this.pending.computeIfAbsent(signature, unused ->
			CompletableFuture.supplyAsync(() -> {
				try {
					return PreviewAnalyzer.analyze(PreviewSnapshot.capture(sourceStack, result, tooltipLines));
				} catch (Exception exception) {
					PeekConstants.LOGGER.warn("Failed to analyze preview for {}", sourceStack, exception);
					return new PreviewAnalysis(0L, 0L, null, null);
				}
			}, EXECUTOR)
		);
		if (!future.isDone()) {
			return null;
		}

		try {
			PreviewAnalysis analysis = future.join();
			synchronized (this.completed) {
				this.completed.put(signature, analysis);
			}
			return analysis;
		} finally {
			this.pending.remove(signature);
		}
	}

	public void clear() {
		synchronized (this.completed) {
			this.completed.clear();
		}
		this.pending.clear();
	}

	public static long signature(ItemStack sourceStack, PreviewResult result, List<Component> tooltipLines) {
		long hash = 1125899906842597L;
		hash = hash * 31L + ItemStack.hashItemAndComponents(sourceStack);
		hash = hash * 31L + result.columns();
		hash = hash * 31L + result.rows();

		for (ItemStack stack : result.slots()) {
			hash = hash * 31L + ItemStack.hashItemAndComponents(stack);
		}
		for (Component line : tooltipLines) {
			hash = hash * 31L + line.getString().hashCode();
		}

		return hash;
	}
}
