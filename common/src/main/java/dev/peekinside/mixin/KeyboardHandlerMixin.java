package dev.peekinside.mixin;

import dev.peekinside.render.PreviewState;
import dev.peekinside.search.SearchFilter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.glfw.GLFW;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
	private void peekinside$charTyped(long window, CharacterEvent event, CallbackInfo ci) {
		if (!PreviewState.shouldCaptureTextInput() || !event.isAllowedChatCharacter()) {
			return;
		}

		SearchFilter.INSTANCE.append((char) event.codepoint());
		ci.cancel();
	}

	@Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
	private void peekinside$keyPress(long window, int action, KeyEvent event, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen instanceof AbstractContainerScreen<?> && PreviewState.consumeLockKey(event, action)) {
			ci.cancel();
			return;
		}

		if (!PreviewState.shouldCaptureTextInput() || action == GLFW.GLFW_RELEASE) {
			return;
		}

		int key = event.key();
		if (key == GLFW.GLFW_KEY_ESCAPE) {
			PreviewState.clearLockedPreview();
			ci.cancel();
			return;
		}

		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			SearchFilter.INSTANCE.backspace();
			ci.cancel();
			return;
		}

		ci.cancel();
	}
}
