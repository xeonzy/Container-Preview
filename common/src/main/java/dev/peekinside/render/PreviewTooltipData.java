package dev.peekinside.render;

import dev.peekinside.api.PreviewResult;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import dev.peekinside.search.SearchFilter;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jspecify.annotations.Nullable;

public record PreviewTooltipData(
	ItemStack sourceStack,
	PreviewResult result,
	SearchFilter searchFilter,
	PreviewPalette palette,
	List<Component> tooltipLines,
	boolean detailMode,
	@Nullable PreviewAnalysis analysis
) implements TooltipComponent {
}
