package dev.peekinside.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.peekinside.PeekInside;

public final class PeekConfigScreen implements ModMenuApi {
	private static final String DISABLE_CONFIG_SCREEN_ENV = "PEEKINSIDE_DISABLE_CONFIG_SCREEN";
	private static final String FORCE_FALLBACK_CONFIG_SCREEN_ENV = "PEEKINSIDE_FORCE_FALLBACK_CONFIG_SCREEN";

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (flag(DISABLE_CONFIG_SCREEN_ENV)) {
			PeekInside.LOGGER.warn("Disabling Mod Menu config screen because {} is enabled", DISABLE_CONFIG_SCREEN_ENV);
			return screen -> null;
		}

		if (flag(FORCE_FALLBACK_CONFIG_SCREEN_ENV)) {
			PeekInside.LOGGER.warn("Forcing fallback config screen because {} is enabled", FORCE_FALLBACK_CONFIG_SCREEN_ENV);
			return PeekFallbackConfigScreen::create;
		}

		if (YaclScreenProvider.isAvailable()) {
			return parent -> YaclScreenProvider.create(parent);
		}

		return parent -> PeekFallbackConfigScreen.create(parent);
	}

	private static boolean flag(String key) {
		return Boolean.parseBoolean(System.getenv(key));
	}
}
