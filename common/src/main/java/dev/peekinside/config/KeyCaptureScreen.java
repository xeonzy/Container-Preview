package dev.peekinside.config;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class KeyCaptureScreen extends Screen {
	private final Screen parent;
	private final Component prompt;
	private final Component valueLabel;
	private final Consumer<InputConstants.Key> onSelect;

	private KeyCaptureScreen(Screen parent, Component title, Component prompt, Component valueLabel, Consumer<InputConstants.Key> onSelect) {
		super(title);
		this.parent = parent;
		this.prompt = prompt;
		this.valueLabel = valueLabel;
		this.onSelect = onSelect;
	}

	public static void open(Screen parent, Component title, Component prompt, Component valueLabel, Consumer<InputConstants.Key> onSelect) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> minecraft.setScreen(new KeyCaptureScreen(parent, title, prompt, valueLabel, onSelect)));
	}

	@Override
	protected void init() {
		this.addRenderableWidget(new MouseCaptureWidget(0, 0, this.width, this.height));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fillGradient(0, 0, this.width, this.height, 0xE0141414, 0xE0080808);
		graphics.fill(this.width / 2 - 120, this.height / 2 - 52, this.width / 2 + 120, this.height / 2 + 52, 0xCC000000);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 34, 0xFFFFFFFF);
		graphics.drawCenteredString(this.font, this.prompt, this.width / 2, this.height / 2 - 10, 0xFFE0E0E0);
		graphics.drawCenteredString(this.font, this.valueLabel, this.width / 2, this.height / 2 + 10, 0xFFB0B0B0);
		graphics.drawCenteredString(this.font, Component.translatable("peekinside.key_capture_cancel"), this.width / 2, this.height / 2 + 28, 0xFF909090);
		super.render(graphics, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int keyCode = event.key();
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.closeWithoutChange();
			return true;
		}

		if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
			this.acceptKey(InputConstants.UNKNOWN);
			return true;
		}

		this.acceptKey(InputConstants.getKey(event));
		return true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(this.parent);
	}

	private void acceptKey(InputConstants.Key key) {
		this.onSelect.accept(key);
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> minecraft.setScreen(this.parent));
	}

	private void closeWithoutChange() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.execute(() -> minecraft.setScreen(this.parent));
	}

	private final class MouseCaptureWidget extends AbstractWidget {
		private MouseCaptureWidget(int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty());
			this.visible = true;
			this.active = true;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean inside) {
			if (inside) {
				KeyCaptureScreen.this.acceptKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
				return true;
			}

			return false;
		}
	}
}
