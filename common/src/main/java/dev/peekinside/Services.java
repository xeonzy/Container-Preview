package dev.peekinside;

import java.util.ServiceLoader;

public class Services {
	public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

	public static <T> T load(Class<T> clazz) {
		return ServiceLoader.load(clazz)
			.findFirst()
			.orElseThrow(() -> new RuntimeException("Failed to load service of type " + clazz.getName()));
	}
}
