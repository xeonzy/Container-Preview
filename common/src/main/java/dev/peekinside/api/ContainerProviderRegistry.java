package dev.peekinside.api;

import dev.peekinside.PeekConstants;

import dev.peekinside.PeekInside;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

public final class ContainerProviderRegistry {
	public static final ContainerProviderRegistry INSTANCE = new ContainerProviderRegistry();

	private final List<Entry> providers = new ArrayList<>();

	private ContainerProviderRegistry() {
	}

	public synchronized void register(Identifier id, int priority, ContainerProvider provider) {
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(provider, "provider");
		this.providers.add(new Entry(priority, id, provider));
		this.providers.sort(Entry::compareTo);
	}

	public @Nullable PreviewResult resolve(PreviewRequest request) {
		for (Entry entry : this.providers) {
			PreviewResult result;
			try {
				result = entry.provider().provide(request);
			} catch (Exception exception) {
				PeekConstants.LOGGER.warn("Preview provider {} failed for {}", entry.id(), request.stack(), exception);
				continue;
			}

			if (result != null) {
				return result;
			}
		}

		return null;
	}

	private record Entry(int priority, Identifier id, ContainerProvider provider) implements Comparable<Entry> {
		@Override
		public int compareTo(Entry other) {
			return Integer.compare(this.priority, other.priority);
		}
	}
}
