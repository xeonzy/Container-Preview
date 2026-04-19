package dev.peekinside.config;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.lwjgl.glfw.GLFW;

public final class KeyNameUtil {
	private static final Map<String, InputConstants.Key> ALIASES = new HashMap<>();
	private static final Map<String, String> DISPLAY_NAMES = new HashMap<>();

	static {
		register("left shift", GLFW.GLFW_KEY_LEFT_SHIFT);
		register("right shift", GLFW.GLFW_KEY_RIGHT_SHIFT);
		register("left control", GLFW.GLFW_KEY_LEFT_CONTROL);
		register("right control", GLFW.GLFW_KEY_RIGHT_CONTROL);
		register("left alt", GLFW.GLFW_KEY_LEFT_ALT);
		register("right alt", GLFW.GLFW_KEY_RIGHT_ALT);
		register("space", GLFW.GLFW_KEY_SPACE);
		register("enter", GLFW.GLFW_KEY_ENTER);
		register("tab", GLFW.GLFW_KEY_TAB);
		register("escape", GLFW.GLFW_KEY_ESCAPE);
		register("backspace", GLFW.GLFW_KEY_BACKSPACE);
		register("up", GLFW.GLFW_KEY_UP);
		// The user-friendly labels below are what we persist in config.
		register("down", GLFW.GLFW_KEY_DOWN);
		register("left", GLFW.GLFW_KEY_LEFT);
		register("right", GLFW.GLFW_KEY_RIGHT);
		register("home", GLFW.GLFW_KEY_HOME);
		register("end", GLFW.GLFW_KEY_END);
		register("page up", GLFW.GLFW_KEY_PAGE_UP);
		register("page down", GLFW.GLFW_KEY_PAGE_DOWN);
		register("insert", GLFW.GLFW_KEY_INSERT);
		register("delete", GLFW.GLFW_KEY_DELETE);

		for (char letter = 'A'; letter <= 'Z'; letter++) {
			register(String.valueOf(Character.toLowerCase(letter)), GLFW.GLFW_KEY_A + (letter - 'A'));
		}

		for (int digit = 0; digit <= 9; digit++) {
			register(Integer.toString(digit), GLFW.GLFW_KEY_0 + digit);
		}

		for (int key = 1; key <= 12; key++) {
			register("f" + key, GLFW.GLFW_KEY_F1 + (key - 1));
		}
	}

	private KeyNameUtil() {
	}

	static String displayName(String serializedKey) {
		if (serializedKey == null || serializedKey.isBlank()) {
			return "Unknown";
		}

		String normalized = normalize(serializedKey);
		String mapped = DISPLAY_NAMES.get(normalized);
		return mapped == null ? prettify(normalized) : mapped;
	}

	public static InputConstants.Key resolve(String serializedKey, InputConstants.Key fallback) {
		if (serializedKey == null || serializedKey.isBlank()) {
			return fallback;
		}

		try {
			return InputConstants.getKey(serializedKey);
		} catch (Exception ignored) {
			InputConstants.Key mapped = ALIASES.get(normalize(serializedKey));
			return mapped == null ? fallback : mapped;
		}
	}

	static String normalizeStored(String serializedKey, String fallbackFriendlyName) {
		String key = serializedKey == null || serializedKey.isBlank() ? fallbackFriendlyName : serializedKey;
		String normalized = normalize(key);
		String mapped = DISPLAY_NAMES.get(normalized);
		return mapped == null ? prettify(normalized) : mapped;
	}

	private static void register(String label, int glfwKey) {
		InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(glfwKey);
		String normalized = normalize(label);
		String displayName = prettify(normalized);
		ALIASES.put(normalized, key);
		ALIASES.put(normalized.replace(" ", ""), key);
		ALIASES.put(normalized.replace(" ", "_"), key);
		DISPLAY_NAMES.put(normalized, displayName);
		DISPLAY_NAMES.put(normalized.replace(" ", ""), displayName);
		DISPLAY_NAMES.put(normalized.replace(" ", "_"), displayName);
	}

	private static String normalize(String value) {
		return value.toLowerCase(Locale.ROOT)
			.replace("key.keyboard.", "")
			.replace("key.mouse.", "")
			.replace('.', ' ')
			.replace('_', ' ')
			.trim();
	}

	private static String prettify(String normalized) {
		if (normalized.isBlank()) {
			return "Unknown";
		}

		String[] parts = normalized.split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (int index = 0; index < parts.length; index++) {
			if (index > 0) {
				builder.append(' ');
			}

			builder.append(prettifyWord(parts[index]));
		}

		return builder.toString();
	}

	private static String prettifyWord(String word) {
		if (word.isEmpty()) {
			return word;
		}

		if (word.length() == 1 && Character.isLetterOrDigit(word.charAt(0))) {
			return word.toUpperCase(Locale.ROOT);
		}

		if (word.startsWith("f") && word.length() > 1 && word.substring(1).chars().allMatch(Character::isDigit)) {
			return "F" + word.substring(1);
		}

		if (word.equals("kp")) {
			return "KP";
		}

		return Character.toUpperCase(word.charAt(0)) + word.substring(1);
	}
}
