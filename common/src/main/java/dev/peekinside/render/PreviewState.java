package dev.peekinside.render;

import dev.peekinside.api.ContainerProviderRegistry;
import dev.peekinside.api.PreviewRequest;
import dev.peekinside.api.PreviewResult;
import dev.peekinside.config.PeekConfig;
import dev.peekinside.search.SearchFilter;
import java.util.ArrayList;
import java.util.List;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("null")
public final class PreviewState {
	public static volatile @Nullable PreviewTooltipData pendingData;
	public static volatile boolean previewActive;

	private static @Nullable PreviewTooltipData lockedData;
	private static @Nullable Screen lockedScreen;
	private static int lockedMouseX;
	private static int lockedMouseY;
	private static long lockedSignature = Long.MIN_VALUE;
	private static boolean detailModeActive;
	private static @Nullable PreviewTooltipData lastHoverData;
	private static @Nullable Screen lastHoverScreen;
	private static int lastHoverMouseX;
	private static int lastHoverMouseY;
	private static boolean lockKeyHeld;
	private static int scrollOffset;
	private static long scrollSignature = Long.MIN_VALUE;
	private static @Nullable Screen boundsScreen;
	private static int boundsX;
	private static int boundsY;
	private static int boundsWidth;
	private static int boundsHeight;
	private static int totalRows;
	private static int visibleRows;

	private PreviewState() {
	}

	public static void beginFrame(Screen screen, boolean detailMode) {
		pendingData = null;
		previewActive = false;
		detailModeActive = detailMode;
		boundsScreen = null;
		boundsWidth = 0;
		boundsHeight = 0;
		refreshLockState(Minecraft.getInstance());

		if (!lockKeyHeld) {
			clearLockedPreview();
		}

		if (lockedScreen != null && lockedScreen != screen) {
			clearAll();
			return;
		}

		refreshLockedPreview();

		if (lastHoverScreen != null && lastHoverScreen != screen) {
			clearLastHover();
		}
	}

	public static @Nullable PreviewResult resolvePreview(
		ItemStack stack,
		@Nullable LocalPlayer player,
		@Nullable Screen screen,
		boolean fullMode
	) {
		if (stack.isEmpty()) {
			return null;
		}

		PreviewRequest request = new PreviewRequest(stack, player, screen, fullMode);
		PreviewResult result = shouldBypassCache(stack)
			? ContainerProviderRegistry.INSTANCE.resolve(request)
			: PreviewCache.INSTANCE.getOrCompute(stack, () -> ContainerProviderRegistry.INSTANCE.resolve(request));

		if (result == null) {
			return null;
		}

		return result;
	}

	public static void showHoverPreview(
		ItemStack stack,
		PreviewResult result,
		Screen screen,
		List<Component> tooltipLines,
		boolean lockPreview,
		boolean detailMode,
		int mouseX,
		int mouseY
	) {
		PreviewAnalysis analysis = PreviewAnalysisCache.INSTANCE.getOrSchedule(stack, result, tooltipLines);
		PreviewTooltipData tooltipData = new PreviewTooltipData(
			stack.copy(),
			result,
			SearchFilter.INSTANCE,
			PreviewPaletteResolver.resolve(stack),
			List.copyOf(tooltipLines),
			detailMode,
			analysis
		);
		pendingData = tooltipData;
		previewActive = true;
		lastHoverData = tooltipData;
		lastHoverScreen = screen;
		lastHoverMouseX = mouseX;
		lastHoverMouseY = mouseY;

		if (lockPreview) {
			lockPreview(tooltipData, screen, mouseX, mouseY);
		}
	}

	public static boolean renderLockedTooltip(Minecraft minecraft, Screen screen, Font font, GuiGraphics graphics) {
		refreshLockedPreview();

		if (lockedData == null || lockedScreen != screen || minecraft.level == null) {
			return false;
		}

		List<Component> tooltipLines = PreviewTooltipTextFilter.filter(lockedData.tooltipLines());
		List<ClientTooltipComponent> tooltipComponents = new ArrayList<>(tooltipLines.size() + 1);
		for (Component line : tooltipLines) {
			tooltipComponents.add(ClientTooltipComponent.create(line.getVisualOrderText()));
		}
		tooltipComponents.add(ClientTooltipComponent.create(lockedData));
		graphics.renderTooltip(
			font,
			tooltipComponents,
			lockedMouseX,
			lockedMouseY,
			net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE,
			lockedData.sourceStack().get(net.minecraft.core.component.DataComponents.TOOLTIP_STYLE)
		);
		previewActive = true;
		return true;
	}

	public static boolean consumeLockKey(KeyEvent event, int action) {
		if (!dev.peekinside.PeekKeys.matchesLock(event)) {
			return false;
		}

		if (action == GLFW.GLFW_RELEASE) {
			lockKeyHeld = false;
			clearLockedPreview();
		} else if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
			lockKeyHeld = true;
			Screen currentScreen = Minecraft.getInstance().screen;
			if (currentScreen != null) {
				lockCurrentHover(currentScreen);
			}
		}
		return true;
	}

	public static boolean hasInteractivePreview() {
		return previewActive || lockedData != null;
	}

	public static boolean hasLockedPreview() {
		return lockedData != null;
	}

	public static boolean shouldCaptureTextInput() {
		return lockKeyHeld && hasInteractivePreview();
	}

	public static boolean isLockKeyHeld() {
		return lockKeyHeld;
	}

	public static void clearTransient() {
		pendingData = null;
	}

	public static void clearLockedPreview() {
		lockedData = null;
		lockedScreen = null;
		lockedMouseX = 0;
		lockedMouseY = 0;
		lockedSignature = Long.MIN_VALUE;
		resetScroll(Long.MIN_VALUE);
		NestedPreviewState.INSTANCE.clear();
		SearchFilter.INSTANCE.clear();
	}

	public static void clearAll() {
		clearTransient();
		previewActive = false;
		clearLockedPreview();
		clearLastHover();
		lockKeyHeld = false;
	}

	public static void registerPanelBounds(Screen screen, long signature, int x, int y, int width, int height, int contentRows, int visibleContentRows) {
		if (scrollSignature != signature) {
			resetScroll(signature);
		}

		boundsScreen = screen;
		boundsX = x;
		boundsY = y;
		boundsWidth = width;
		boundsHeight = height;
		totalRows = Math.max(0, contentRows);
		visibleRows = Math.max(0, visibleContentRows);
	}

	public static int scrollOffset() {
		return scrollOffset;
	}

	public static boolean handleScroll(Screen screen, double mouseX, double mouseY, double amountY) {
		if (!hasInteractivePreview() || boundsScreen != screen || totalRows <= visibleRows || amountY == 0.0D) {
			return false;
		}
		if (mouseX < boundsX || mouseY < boundsY || mouseX >= boundsX + boundsWidth || mouseY >= boundsY + boundsHeight) {
			return false;
		}

		int nextOffset = scrollOffset + (amountY < 0.0D ? 1 : -1);
		scrollOffset = clamp(nextOffset, 0, Math.max(0, totalRows - visibleRows));
		return true;
	}

	private static boolean shouldBypassCache(ItemStack stack) {
		return stack.is(Items.ENDER_CHEST);
	}

	private static void resetScroll(long signature) {
		scrollSignature = signature;
		scrollOffset = 0;
	}

	private static void refreshLockState(Minecraft minecraft) {
		lockKeyHeld = dev.peekinside.PeekKeys.isBindingDown(
			minecraft,
			PeekConfig.INSTANCE.lockKey,
			InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LALT)
		);
	}

	private static void lockCurrentHover(Screen currentScreen) {
		if (lastHoverData != null && lastHoverScreen == currentScreen) {
			lockPreview(lastHoverData, currentScreen, lastHoverMouseX, lastHoverMouseY);
			return;
		}

		if (pendingData != null && lastHoverScreen == currentScreen) {
			lockPreview(pendingData, currentScreen, lastHoverMouseX, lastHoverMouseY);
		}
	}

	private static void lockPreview(PreviewTooltipData tooltipData, Screen screen, int mouseX, int mouseY) {
		lockedData = tooltipData;
		lockedScreen = screen;
		lockedMouseX = mouseX;
		lockedMouseY = mouseY;
		lockedSignature = PreviewAnalysisCache.signature(tooltipData.sourceStack(), tooltipData.result(), tooltipData.tooltipLines());
		resetScroll(lockedSignature);
	}

	public static void refreshLockedPreview() {
		PreviewTooltipData current = lockedData;
		Screen screen = lockedScreen;
		if (current == null || screen == null) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null) {
			return;
		}

		PreviewResult refreshedResult = resolvePreview(current.sourceStack(), minecraft.player, screen, detailModeActive);
		if (refreshedResult == null) {
			clearLockedPreview();
			return;
		}

		long refreshedSignature = PreviewAnalysisCache.signature(current.sourceStack(), refreshedResult, current.tooltipLines());
		if (current.detailMode() == detailModeActive && refreshedSignature == lockedSignature && current.result() == refreshedResult) {
			return;
		}

		PreviewAnalysis refreshedAnalysis = PreviewAnalysisCache.INSTANCE.getOrSchedule(
			current.sourceStack(),
			refreshedResult,
			current.tooltipLines()
		);
		lockedData = new PreviewTooltipData(
			current.sourceStack(),
			refreshedResult,
			current.searchFilter(),
			current.palette(),
			current.tooltipLines(),
			detailModeActive,
			refreshedAnalysis
		);
		lockedSignature = refreshedSignature;
		resetScroll(refreshedSignature);
	}

	private static void clearLastHover() {
		lastHoverData = null;
		lastHoverScreen = null;
		lastHoverMouseX = 0;
		lastHoverMouseY = 0;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
