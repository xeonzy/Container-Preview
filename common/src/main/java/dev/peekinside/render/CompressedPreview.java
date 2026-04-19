package dev.peekinside.render;

import net.minecraft.world.item.ItemStack;

public record CompressedPreview(
	ItemStack stack,
	long totalCount,
	int stackCount
) {
}
