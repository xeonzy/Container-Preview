package dev.peekinside;

import dev.peekinside.PeekConstants;

import com.mojang.blaze3d.platform.InputConstants;
import dev.peekinside.config.KeyNameUtil;
import dev.peekinside.config.PeekConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import dev.peekinside.Services;

@SuppressWarnings("null")
public final class PeekKeys {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(PeekConstants.MOD_ID, "keys")
	);

	public static final KeyMapping PREVIEW = Services.PLATFORM.registerKeyBinding(
		new KeyMapping(
			"key.peekinside.preview",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_LSHIFT,
			CATEGORY
		)
	);

	public static final KeyMapping DETAIL = Services.PLATFORM.registerKeyBinding(
		new KeyMapping(
			"key.peekinside.detail",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_LCONTROL,
			CATEGORY
		)
	);

	public static final KeyMapping LOCK = Services.PLATFORM.registerKeyBinding(
		new KeyMapping(
			"key.peekinside.lock",
			InputConstants.Type.KEYSYM,
			InputConstants.KEY_LALT,
			CATEGORY
		)
	);

	private PeekKeys() {
	}

	public static void initialize() {
		// Triggers static initialization.
	}

	public static void applyFromConfig() {
		apply(PREVIEW, PeekConfig.INSTANCE.previewKey, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LSHIFT));
		apply(DETAIL, PeekConfig.INSTANCE.detailKey, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LCONTROL));
		apply(LOCK, PeekConfig.INSTANCE.lockKey, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LALT));
	}

	public static boolean matchesLock(KeyEvent event) {
		InputConstants.Key binding = KeyNameUtil.resolve(PeekConfig.INSTANCE.lockKey, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LALT));
		return matchesKey(event, binding);
	}

	public static boolean isDetailModeActive() {
		Minecraft minecraft = Minecraft.getInstance();
		return isBindingDown(minecraft, PeekConfig.INSTANCE.detailKey, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LCONTROL));
	}

	public static boolean isPreviewKeyDown() {
		Minecraft minecraft = Minecraft.getInstance();
		return isBindingDown(minecraft, PeekConfig.INSTANCE.previewKey, InputConstants.Type.KEYSYM.getOrCreate(InputConstants.KEY_LSHIFT));
	}

	private static void apply(KeyMapping mapping, String serialized, InputConstants.Key fallback) {
		mapping.setKey(KeyNameUtil.resolve(serialized, fallback));
	}

	public static boolean isKeyDown(KeyMapping mapping) {
		return Services.PLATFORM.isKeyDown(mapping);
	}

	public static boolean isBindingDown(Minecraft minecraft, String serialized, InputConstants.Key fallback) {
		if (minecraft == null || minecraft.getWindow() == null) {
			return false;
		}

		InputConstants.Key key = KeyNameUtil.resolve(serialized, fallback);
		long window = minecraft.getWindow().handle();
		return switch (key.getType()) {
			case KEYSYM, SCANCODE -> InputConstants.isKeyDown(minecraft.getWindow(), key.getValue());
			case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
		};
	}

	private static boolean matchesKey(KeyEvent event, InputConstants.Key key) {
		return switch (key.getType()) {
			case KEYSYM -> event.key() == key.getValue();
			case SCANCODE -> event.scancode() == key.getValue();
			case MOUSE -> false;
		};
	}
}
