package dev.peekinside.render;

public record PreviewPalette(
	int panelBackgroundColor,
	int borderColor,
	int slotBackgroundColor,
	int hoveredSlotColor,
	int summaryTextColor,
	int searchTextColor
) {
	public static final PreviewPalette DEFAULT = new PreviewPalette(
		0xD81A1A1A,
		0xFF2A2A2A,
		0x1EFFFFFF,
		0x2AFFFFFF,
		0xFFAAAAAA,
		0xFFFFDD55
	);
}
