package dev.peekinside.api;

import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface ContainerProvider {
	@Nullable PreviewResult provide(PreviewRequest request);
}
