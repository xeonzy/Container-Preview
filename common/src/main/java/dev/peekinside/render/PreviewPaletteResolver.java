package dev.peekinside.render;

import dev.peekinside.config.PeekConfig;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

public final class PreviewPaletteResolver {
	private PreviewPaletteResolver() {
	}

	public static PreviewPalette resolve(ItemStack stack) {
		if (!PeekConfig.INSTANCE.tintContainerBackgrounds) {
			return PreviewPalette.DEFAULT;
		}

		@Nullable DyeColor shulkerColor = getShulkerColor(stack.getItem());
		if (shulkerColor != null) {
			return createTintedPalette(shulkerColor.getTextureDiffuseColor());
		}

		if (stack.is(Items.ENDER_CHEST)) {
			return createTintedPalette(0x2F8F77);
		}

		return PreviewPalette.DEFAULT;
	}

	private static PreviewPalette createTintedPalette(int rgb) {
		int panelRgb = mix(rgb, 0x101010, 0.80F);
		int borderRgb = mix(rgb, 0xFFFFFF, 0.18F);
		int slotRgb = mix(rgb, 0xFFFFFF, 0.10F);
		int hoverRgb = mix(rgb, 0xFFFFFF, 0.24F);
		int summaryRgb = mix(rgb, 0xFFFFFF, 0.48F);

		return new PreviewPalette(
			argb(0xE4, panelRgb),
			argb(0xFF, borderRgb),
			argb(0x34, slotRgb),
			argb(0x4A, hoverRgb),
			argb(0xFF, summaryRgb),
			0xFFFFDD55
		);
	}

	private static int mix(int fromRgb, int toRgb, float amount) {
		float clamped = Math.max(0.0F, Math.min(1.0F, amount));
		int fromRed = fromRgb >> 16 & 0xFF;
		int fromGreen = fromRgb >> 8 & 0xFF;
		int fromBlue = fromRgb & 0xFF;
		int toRed = toRgb >> 16 & 0xFF;
		int toGreen = toRgb >> 8 & 0xFF;
		int toBlue = toRgb & 0xFF;

		int red = Math.round(fromRed + (toRed - fromRed) * clamped);
		int green = Math.round(fromGreen + (toGreen - fromGreen) * clamped);
		int blue = Math.round(fromBlue + (toBlue - fromBlue) * clamped);
		return red << 16 | green << 8 | blue;
	}

	private static int argb(int alpha, int rgb) {
		return alpha << 24 | rgb & 0x00FFFFFF;
	}

	private static @Nullable DyeColor getShulkerColor(Item item) {
		if (item == Items.SHULKER_BOX) {
			return DyeColor.PURPLE;
		}
		if (item == Items.WHITE_SHULKER_BOX) {
			return DyeColor.WHITE;
		}
		if (item == Items.ORANGE_SHULKER_BOX) {
			return DyeColor.ORANGE;
		}
		if (item == Items.MAGENTA_SHULKER_BOX) {
			return DyeColor.MAGENTA;
		}
		if (item == Items.LIGHT_BLUE_SHULKER_BOX) {
			return DyeColor.LIGHT_BLUE;
		}
		if (item == Items.YELLOW_SHULKER_BOX) {
			return DyeColor.YELLOW;
		}
		if (item == Items.LIME_SHULKER_BOX) {
			return DyeColor.LIME;
		}
		if (item == Items.PINK_SHULKER_BOX) {
			return DyeColor.PINK;
		}
		if (item == Items.GRAY_SHULKER_BOX) {
			return DyeColor.GRAY;
		}
		if (item == Items.LIGHT_GRAY_SHULKER_BOX) {
			return DyeColor.LIGHT_GRAY;
		}
		if (item == Items.CYAN_SHULKER_BOX) {
			return DyeColor.CYAN;
		}
		if (item == Items.PURPLE_SHULKER_BOX) {
			return DyeColor.PURPLE;
		}
		if (item == Items.BLUE_SHULKER_BOX) {
			return DyeColor.BLUE;
		}
		if (item == Items.BROWN_SHULKER_BOX) {
			return DyeColor.BROWN;
		}
		if (item == Items.GREEN_SHULKER_BOX) {
			return DyeColor.GREEN;
		}
		if (item == Items.RED_SHULKER_BOX) {
			return DyeColor.RED;
		}
		if (item == Items.BLACK_SHULKER_BOX) {
			return DyeColor.BLACK;
		}

		return null;
	}
}
