package dev.peekinside.mixin;

import dev.peekinside.PeekKeys;
import dev.peekinside.api.PreviewResult;
import dev.peekinside.config.PeekConfig;
import dev.peekinside.render.NestedPreviewState;
import dev.peekinside.render.PreviewState;
import dev.peekinside.render.PreviewTooltipTextFilter;
import dev.peekinside.render.PreviewTooltipData;
import dev.peekinside.search.SearchFilter;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
@SuppressWarnings({"null", "unused", "dead code"})
public abstract class HandledScreenMixin extends Screen {
	@Unique
	private static boolean peekinside$lastPreviewActive;

	@Shadow
	protected Slot hoveredSlot;

	@Shadow
	protected abstract List<Component> getTooltipFromContainerItem(ItemStack stack);

	protected HandledScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "render", at = @At("HEAD"))
	private void peekinside$beginFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		PreviewState.beginFrame((Screen) (Object) this, PeekKeys.isDetailModeActive());

		if (this.minecraft == null || this.minecraft.level == null) {
			PreviewState.clearAll();
		}
	}

	@Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
	private void peekinside$preparePreview(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
		PreviewState.clearTransient();

		if (this.minecraft == null || this.minecraft.level == null) {
			return;
		}

		if (PreviewState.hasLockedPreview()) {
			ci.cancel();
			return;
		}

		if (!PeekConfig.INSTANCE.previewEnabled) {
			return;
		}

		if (!PeekConfig.INSTANCE.alwaysShow && !PeekKeys.isPreviewKeyDown() && !PreviewState.isLockKeyHeld()) {
			return;
		}

		Slot slot = this.hoveredSlot;
		if (slot == null || !slot.hasItem()) {
			return;
		}

		ItemStack stack = slot.getItem();
		if (stack.isEmpty() || this.peekinside$isBlacklisted(stack)) {
			return;
		}

		boolean detailMode = PeekKeys.isDetailModeActive();
		PreviewResult result = PreviewState.resolvePreview(
			stack,
			this.minecraft.player,
			this,
			detailMode
		);
		if (result == null) {
			return;
		}

		List<Component> tooltipLines = this.getTooltipFromContainerItem(stack);
		PreviewState.showHoverPreview(
			stack,
			result,
			this,
			tooltipLines,
			PreviewState.isLockKeyHeld(),
			detailMode,
			mouseX,
			mouseY
		);
		PreviewTooltipData tooltipData = PreviewState.pendingData;
		if (tooltipData == null) {
			return;
		}

		graphics.setTooltipForNextFrame(
			this.font,
			PreviewTooltipTextFilter.filter(tooltipData.tooltipLines()),
			Optional.of(tooltipData),
			mouseX,
			mouseY,
			stack.get(DataComponents.TOOLTIP_STYLE)
		);
		ci.cancel();
	}

	@Inject(method = "renderTooltip", at = @At("RETURN"))
	private void peekinside$clearPreview(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
		PreviewState.clearTransient();
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void peekinside$endFrame(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (!PreviewState.previewActive && this.minecraft != null) {
			PreviewState.renderLockedTooltip(this.minecraft, this, this.font, graphics);
		}

		if (peekinside$lastPreviewActive && !PreviewState.hasInteractivePreview()) {
			SearchFilter.INSTANCE.clear();
			NestedPreviewState.INSTANCE.clear();
		}

		peekinside$lastPreviewActive = PreviewState.hasInteractivePreview();
	}

	@Inject(method = "removed", at = @At("HEAD"))
	private void peekinside$removed(CallbackInfo ci) {
		PreviewState.clearAll();
	}

	@Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
	private void peekinside$mouseScrolled(double mouseX, double mouseY, double amountX, double amountY, CallbackInfoReturnable<Boolean> cir) {
		if (PreviewState.handleScroll(this, mouseX, mouseY, amountY)) {
			cir.setReturnValue(true);
		}
	}

	@Unique
	private boolean peekinside$isBlacklisted(ItemStack stack) {
		String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		return PeekConfig.INSTANCE.blacklist.contains(itemId);
	}
}
