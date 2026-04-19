package dev.peekinside.provider;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class GridShapeRegistry {
	public static final GridShapeRegistry INSTANCE = new GridShapeRegistry();

	private final Map<Item, int[]> shapes = new HashMap<>();

	private GridShapeRegistry() {
		this.seedVanilla();
	}

	public void register(Item item, int cols, int rows) {
		Objects.requireNonNull(item, "item");

		if (cols <= 0 || rows <= 0) {
			throw new IllegalArgumentException("Grid shape must be positive");
		}

		this.shapes.put(item, new int[] {cols, rows});
	}

	public void registerIfPresent(String namespace, String path, int cols, int rows) {
		Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
		BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> {
			dev.peekinside.PeekInside.LOGGER.info("Registered grid shape for item: {} ({}x{})", id, cols, rows);
			this.register(item, cols, rows);
		});
	}

	public int getCols(Item item, int slotCount) {
		int[] shape = this.shapes.get(item);
		if (shape != null) {
			return shape[0];
		}
		return guessColumns(slotCount);
	}

	public int getRows(Item item, int slotCount, int cols) {
		int[] shape = this.shapes.get(item);
		if (shape != null) {
			return shape[1];
		}
		return Math.max(1, (slotCount + cols - 1) / cols);
	}

	public static int guessColumns(int slotCount) {
		if (slotCount <= 0) return 9;
		if (slotCount <= 1) return 1;
		if (slotCount == 5) return 5;
		if (slotCount <= 9) return 3;
		if (slotCount % 9 == 0 || slotCount > 27) return 9;
		if (slotCount % 5 == 0 && slotCount <= 15) return 5;
		if (slotCount % 4 == 0 && slotCount <= 16) return 4;
		return 9;
	}

	private void seedVanilla() {
		this.registerShulker(Items.SHULKER_BOX);
		this.registerShulker(Items.WHITE_SHULKER_BOX);
		this.registerShulker(Items.ORANGE_SHULKER_BOX);
		this.registerShulker(Items.MAGENTA_SHULKER_BOX);
		this.registerShulker(Items.LIGHT_BLUE_SHULKER_BOX);
		this.registerShulker(Items.YELLOW_SHULKER_BOX);
		this.registerShulker(Items.LIME_SHULKER_BOX);
		this.registerShulker(Items.PINK_SHULKER_BOX);
		this.registerShulker(Items.GRAY_SHULKER_BOX);
		this.registerShulker(Items.LIGHT_GRAY_SHULKER_BOX);
		this.registerShulker(Items.CYAN_SHULKER_BOX);
		this.registerShulker(Items.PURPLE_SHULKER_BOX);
		this.registerShulker(Items.BLUE_SHULKER_BOX);
		this.registerShulker(Items.BROWN_SHULKER_BOX);
		this.registerShulker(Items.GREEN_SHULKER_BOX);
		this.registerShulker(Items.RED_SHULKER_BOX);
		this.registerShulker(Items.BLACK_SHULKER_BOX);
		this.register(Items.CHEST, 9, 3);
		this.register(Items.TRAPPED_CHEST, 9, 3);
		this.register(Items.BARREL, 9, 3);
		this.register(Items.HOPPER, 5, 1);
		this.register(Items.DISPENSER, 3, 3);
		this.register(Items.DROPPER, 3, 3);
		this.register(Items.FURNACE, 3, 1);
		this.register(Items.BLAST_FURNACE, 3, 1);
		this.register(Items.SMOKER, 3, 1);
		this.register(Items.BREWING_STAND, 5, 1);
	}

	private void registerShulker(Item item) {
		this.register(item, 9, 3);
	}
}
