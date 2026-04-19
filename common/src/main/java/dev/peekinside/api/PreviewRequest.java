package dev.peekinside.api;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record PreviewRequest(
	ItemStack stack,
	@Nullable LocalPlayer player,
	@Nullable Screen screen,
	boolean fullMode
) {
}
