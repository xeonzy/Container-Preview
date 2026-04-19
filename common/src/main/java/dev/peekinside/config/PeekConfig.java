package dev.peekinside.config;

import dev.peekinside.PeekConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import dev.peekinside.PeekKeys;
import dev.peekinside.Services;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class PeekConfig {
	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();

	private static final Path CONFIG_PATH = Services.PLATFORM
		.getConfigDir()
		.resolve(PeekConstants.MOD_ID + ".json");

	public static PeekConfig INSTANCE = new PeekConfig();

	public boolean previewEnabled = true;
	public boolean altLockEnabled = true;
	public boolean compressionEnabled = true;
	@SerializedName(value = "detailPreviewEnabled", alternate = {"legacyDetailShortcutEnabled", "altShiftFullPreviewEnabled"})
	public boolean detailPreviewEnabled = true;
	public boolean nestedPreviewEnabled = true;
	public boolean showSellerMetadata = true;
	public String previewKey = "Left Shift";
	@SerializedName(value = "detailKey", alternate = {"fullPreviewKey"})
	public String detailKey = "Left Control";
	public String lockKey = "Left Alt";
	public int slotSize = 18;
	public int padding = 4;
	public int uiScale = 100;
	public int maxVisibleRows = 6;
	public boolean showSummaryBar = true;
	public boolean showSearchBar = true;
	public boolean showSlotItemCounts = true;
	public boolean compactNumberFormatting = true;
	public boolean alwaysShow = true;
	public boolean showEmpty = true;
	public boolean showEmptyContainerMessage = true;
	public boolean hideVanillaContainerLines = true;
	public boolean tintContainerBackgrounds = true;
	public int maxNestedDepth = 2;
	public int cacheSize = 64;
	public List<String> blacklist = new ArrayList<>();

	public static PeekConfig load() {
		PeekConfig loaded = new PeekConfig();

		try {
			Files.createDirectories(CONFIG_PATH.getParent());

			if (Files.exists(CONFIG_PATH)) {
				String raw = Files.readString(CONFIG_PATH);
				loaded = sanitize(GSON.fromJson(raw, PeekConfig.class));
			} else {
				save(loaded);
			}
		} catch (Exception exception) {
			PeekConstants.LOGGER.warn("Failed to load config from {}", CONFIG_PATH, exception);
			loaded = new PeekConfig();

			try {
				save(loaded);
			} catch (RuntimeException saveException) {
				PeekConstants.LOGGER.warn("Failed to write default config to {}", CONFIG_PATH, saveException);
			}
		}

		INSTANCE = loaded;
		return loaded;
	}

	public static void reload() {
		load();
	}

	public void save() {
		this.captureCurrentKeyBindings();
		save(this);
	}

	public void captureCurrentKeyBindings() {
		this.previewKey = KeyNameUtil.displayName(PeekKeys.PREVIEW.saveString());
		this.detailKey = KeyNameUtil.displayName(PeekKeys.DETAIL.saveString());
		this.lockKey = KeyNameUtil.displayName(PeekKeys.LOCK.saveString());
	}

	private static void save(PeekConfig config) {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			Files.writeString(CONFIG_PATH, GSON.toJson(sanitize(config)));
		} catch (IOException exception) {
			throw new RuntimeException("Failed to save config to " + CONFIG_PATH, exception);
		}
	}

	private static PeekConfig sanitize(@Nullable PeekConfig config) {
		PeekConfig sanitized = config == null ? new PeekConfig() : config;

		sanitized.previewKey = KeyNameUtil.normalizeStored(sanitized.previewKey, "Left Shift");
		sanitized.detailKey = KeyNameUtil.normalizeStored(sanitized.detailKey, "Left Control");
		sanitized.lockKey = KeyNameUtil.normalizeStored(sanitized.lockKey, "Left Alt");

		sanitized.blacklist = sanitized.blacklist == null ? new ArrayList<>() : new ArrayList<>(sanitized.blacklist);
		return sanitized;
	}
}
