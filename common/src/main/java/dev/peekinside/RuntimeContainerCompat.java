package dev.peekinside;

import dev.peekinside.provider.GridShapeRegistry;
import java.util.ArrayList;
import java.util.List;

public final class RuntimeContainerCompat {
	private static final String[] TIERED_BACKPACK_NAMESPACES = {"tiered_backpacks", "tiered-backpacks"};

	private RuntimeContainerCompat() {
	}

	public static void register() {
		GridShapeRegistry shapes = GridShapeRegistry.INSTANCE;
		List<String> loadedMods = new ArrayList<>();

		if (Services.PLATFORM.isModLoaded("mochila")) {
			loadedMods.add("mochila");
			registerMochila(shapes);
		}
		if (Services.PLATFORM.isModLoaded("travelersbackpack")) {
			loadedMods.add("travelersbackpack");
		}
		if (Services.PLATFORM.isModLoaded("sophisticatedbackpacks")) {
			loadedMods.add("sophisticatedbackpacks");
			registerSophisticatedBackpacks(shapes);
		}
		if (Services.PLATFORM.isModLoaded("backpacked")) {
			loadedMods.add("backpacked");
			shapes.registerIfPresent("backpacked", "backpack", 9, 2);
		}
		if (Services.PLATFORM.isModLoaded("tiered_backpacks") || Services.PLATFORM.isModLoaded("tiered-backpacks")) {
			loadedMods.add("tiered_backpacks");
			registerTieredBackpacks(shapes);
		}
		if (Services.PLATFORM.isModLoaded("yyzsbackpack")) {
			loadedMods.add("yyzsbackpack");
			registerBlankpack(shapes);
		}
		if (Services.PLATFORM.isModLoaded("packed-chest")) {
			loadedMods.add("packed-chest");
		}
		if (Services.PLATFORM.isModLoaded("pocketmachines")) {
			loadedMods.add("pocketmachines");
		}

		if (loadedMods.isEmpty()) {
			PeekInside.LOGGER.info("Container compat bootstrap complete: no runtime container mods detected");
		} else {
			PeekInside.LOGGER.info("Container compat bootstrap complete: {}", String.join(", ", loadedMods));
		}
	}

	private static void registerSophisticatedBackpacks(GridShapeRegistry shapes) {
		shapes.registerIfPresent("sophisticatedbackpacks", "backpack", 9, 3);
		shapes.registerIfPresent("sophisticatedbackpacks", "iron_backpack", 9, 6);
		shapes.registerIfPresent("sophisticatedbackpacks", "gold_backpack", 9, 9);
		shapes.registerIfPresent("sophisticatedbackpacks", "diamond_backpack", 12, 10);
		shapes.registerIfPresent("sophisticatedbackpacks", "netherite_backpack", 12, 10);
	}

	private static void registerMochila(GridShapeRegistry shapes) {
		registerNamedBackpackSet(shapes, "mochila", 9, 2, "leather_backpack");
		registerNamedBackpackSet(shapes, "mochila", 9, 4, "iron_backpack");
		registerNamedBackpackSet(shapes, "mochila", 9, 6, "gold_backpack", "diamond_backpack", "netherite_backpack", "ender_backpack");

		for (String color : new String[] {"black_", "blue_", "brown_", "cyan_", "gray_", "green_", "light_blue_", "light_gray_", "lime_", "magenta_", "orange_", "pink_", "purple_", "red_", "white_", "yellow_"}) {
			registerNamedBackpackSet(shapes, "mochila", 9, 2, color + "leather_backpack");
			registerNamedBackpackSet(shapes, "mochila", 9, 4, color + "iron_backpack");
			registerNamedBackpackSet(shapes, "mochila", 9, 6, color + "gold_backpack", color + "diamond_backpack", color + "netherite_backpack");
		}
	}

	private static void registerTieredBackpacks(GridShapeRegistry shapes) {
		registerAnyNamespace(shapes, 9, 1, TIERED_BACKPACK_NAMESPACES, "leather_backpack");
		registerAnyNamespace(shapes, 9, 2, TIERED_BACKPACK_NAMESPACES, "copper_backpack");
		registerAnyNamespace(shapes, 9, 3, TIERED_BACKPACK_NAMESPACES, "iron_backpack");
		registerAnyNamespace(shapes, 9, 4, TIERED_BACKPACK_NAMESPACES, "gold_backpack");
		registerAnyNamespace(shapes, 9, 6, TIERED_BACKPACK_NAMESPACES, "diamond_backpack");
		registerAnyNamespace(shapes, 9, 6, TIERED_BACKPACK_NAMESPACES, "netherite_backpack");
	}

	private static void registerBlankpack(GridShapeRegistry shapes) {
		shapes.registerIfPresent("yyzsbackpack", "iron_backpack", 9, 2);
		shapes.registerIfPresent("yyzsbackpack", "gold_backpack", 9, 4);
		shapes.registerIfPresent("yyzsbackpack", "diamond_backpack", 9, 6);
		shapes.registerIfPresent("yyzsbackpack", "netherite_backpack", 9, 6);
	}

	private static void registerNamedBackpackSet(GridShapeRegistry shapes, String namespace, int cols, int rows, String... itemPaths) {
		for (String itemPath : itemPaths) {
			shapes.registerIfPresent(namespace, itemPath, cols, rows);
		}
	}

	private static void registerAnyNamespace(GridShapeRegistry shapes, int cols, int rows, String[] namespaces, String itemPath) {
		for (String namespace : namespaces) {
			shapes.registerIfPresent(namespace, itemPath, cols, rows);
		}
	}
}
