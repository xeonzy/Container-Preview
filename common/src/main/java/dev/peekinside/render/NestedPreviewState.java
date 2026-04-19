package dev.peekinside.render;

import dev.peekinside.api.PreviewResult;
import dev.peekinside.config.PeekConfig;
import java.util.ArrayDeque;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

public final class NestedPreviewState {
	public static final NestedPreviewState INSTANCE = new NestedPreviewState();

	private final Deque<PreviewResult> stack = new ArrayDeque<>();
	private int hoveredSlotIndex = -1;

	private NestedPreviewState() {
	}

	public void push(PreviewResult result) {
		if (this.stack.size() >= PeekConfig.INSTANCE.maxNestedDepth) {
			return;
		}

		this.stack.addLast(result);
	}

	public void pop() {
		this.stack.pollLast();
		if (this.stack.isEmpty()) {
			this.hoveredSlotIndex = -1;
		}
	}

	public void clear() {
		this.stack.clear();
		this.hoveredSlotIndex = -1;
	}

	public @Nullable PreviewResult current() {
		return this.stack.peekLast();
	}

	public void setHoveredSlotIndex(int hoveredSlotIndex) {
		this.hoveredSlotIndex = hoveredSlotIndex;
	}

	public int hoveredSlotIndex() {
		return this.hoveredSlotIndex;
	}
}
