package dev.peekinside.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.peekinside.PeekKeys;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@SuppressWarnings("null")
final class PeekYaclConfigScreen {
	private PeekYaclConfigScreen() {
	}

	static Screen create(Screen parent) {
		PeekConfig config = PeekConfig.INSTANCE;
		return YetAnotherConfigLib.createBuilder()
			.title(Component.translatable("config.peekinside.title"))
			.category(controlsCategory(config))
			.category(generalCategory(config))
			.category(layoutCategory(config))
			.category(advancedCategory(config))
			.save(() -> {
				PeekKeys.applyFromConfig();
				config.save();
				KeyMapping.resetMapping();
				Minecraft.getInstance().options.save();
			})
			.build()
			.generateScreen(parent);
	}

	private static ConfigCategory controlsCategory(PeekConfig config) {
		return ConfigCategory.createBuilder()
			.name(Component.translatable("config.peekinside.category.controls"))
			.tooltip(Component.translatable(
				"config.peekinside.controls_hint",
				PeekKeys.PREVIEW.getTranslatedKeyMessage(),
				PeekKeys.LOCK.getTranslatedKeyMessage(),
				PeekKeys.DETAIL.getTranslatedKeyMessage()
			))
			.option(keyButton(
				config,
				"config.peekinside.preview_key",
				"config.peekinside.preview_key.desc",
				"config.peekinside.preview_key.capture",
				() -> config.previewKey,
				value -> config.previewKey = value,
				key -> PeekKeys.PREVIEW.setKey(key)
			))
			.option(keyButton(
				config,
				"config.peekinside.lock_key",
				"config.peekinside.lock_key.desc",
				"config.peekinside.lock_key.capture",
				() -> config.lockKey,
				value -> config.lockKey = value,
				key -> PeekKeys.LOCK.setKey(key)
			))
			.option(keyButton(
				config,
				"config.peekinside.detail_key",
				"config.peekinside.detail_key.desc",
				"config.peekinside.detail_key.capture",
				() -> config.detailKey,
				value -> config.detailKey = value,
				key -> PeekKeys.DETAIL.setKey(key)
			))
			.build();
	}

	private static ConfigCategory generalCategory(PeekConfig config) {
		return ConfigCategory.createBuilder()
			.name(Component.translatable("config.peekinside.category.general"))
			.option(booleanOption("config.peekinside.preview_enabled", "config.peekinside.preview_enabled.desc", () -> config.previewEnabled, value -> config.previewEnabled = value, true))
			.option(booleanOption("config.peekinside.hold_to_preview", "config.peekinside.hold_to_preview.desc", () -> !config.alwaysShow, value -> config.alwaysShow = !value, false))
			.option(booleanOption("config.peekinside.locking_enabled", "config.peekinside.locking_enabled.desc", () -> config.altLockEnabled, value -> config.altLockEnabled = value, true))
			.option(booleanOption("config.peekinside.compression_enabled", "config.peekinside.compression_enabled.desc", () -> config.compressionEnabled, value -> config.compressionEnabled = value, true))
			.option(booleanOption("config.peekinside.detail_preview_enabled", "config.peekinside.detail_preview_enabled.desc", () -> config.detailPreviewEnabled, value -> config.detailPreviewEnabled = value, true))
			.option(booleanOption("config.peekinside.nested_previews_enabled", "config.peekinside.nested_previews_enabled.desc", () -> config.nestedPreviewEnabled, value -> config.nestedPreviewEnabled = value, true))
			.option(booleanOption("config.peekinside.show_market_info", "config.peekinside.show_market_info.desc", () -> config.showSellerMetadata, value -> config.showSellerMetadata = value, true))
			.option(booleanOption("config.peekinside.show_statistics_bar", "config.peekinside.show_statistics_bar.desc", () -> config.showSummaryBar, value -> config.showSummaryBar = value, true))
			.option(booleanOption("config.peekinside.show_search_filter", "config.peekinside.show_search_filter.desc", () -> config.showSearchBar, value -> config.showSearchBar = value, true))
			.option(booleanOption("config.peekinside.show_slot_counts", "config.peekinside.show_slot_counts.desc", () -> config.showSlotItemCounts, value -> config.showSlotItemCounts = value, true))
			.option(booleanOption("config.peekinside.compact_number_formatting", "config.peekinside.compact_number_formatting.desc", () -> config.compactNumberFormatting, value -> config.compactNumberFormatting = value, true))
			.option(booleanOption("config.peekinside.show_empty_containers", "config.peekinside.show_empty_containers.desc", () -> config.showEmpty, value -> config.showEmpty = value, true))
			.option(booleanOption("config.peekinside.show_empty_label", "config.peekinside.show_empty_label.desc", () -> config.showEmptyContainerMessage, value -> config.showEmptyContainerMessage = value, true))
			.option(booleanOption("config.peekinside.hide_vanilla_lines", "config.peekinside.hide_vanilla_lines.desc", () -> config.hideVanillaContainerLines, value -> config.hideVanillaContainerLines = value, true))
			.option(booleanOption("config.peekinside.tint_background", "config.peekinside.tint_background.desc", () -> config.tintContainerBackgrounds, value -> config.tintContainerBackgrounds = value, true))
			.build();
	}

	private static ConfigCategory layoutCategory(PeekConfig config) {
		return ConfigCategory.createBuilder()
			.name(Component.translatable("config.peekinside.category.layout"))
			.option(intOption("config.peekinside.slot_size", "config.peekinside.slot_size.desc", () -> config.slotSize, value -> config.slotSize = value, 18, 16, 64))
			.option(intOption("config.peekinside.padding", "config.peekinside.padding.desc", () -> config.padding, value -> config.padding = value, 4, 0, 32))
			.option(intOption("config.peekinside.ui_scale", "config.peekinside.ui_scale.desc", () -> config.uiScale, value -> config.uiScale = value, 100, 100, 300))
			.option(intOption("config.peekinside.max_visible_rows", "config.peekinside.max_visible_rows.desc", () -> config.maxVisibleRows, value -> config.maxVisibleRows = value, 6, 1, 9))
			.build();
	}

	private static ConfigCategory advancedCategory(PeekConfig config) {
		return ConfigCategory.createBuilder()
			.name(Component.translatable("config.peekinside.category.advanced"))
			.option(intOption("config.peekinside.max_nested_depth", "config.peekinside.max_nested_depth.desc", () -> config.maxNestedDepth, value -> config.maxNestedDepth = value, 2, 0, 8))
			.build();
	}

	private static Option<Boolean> booleanOption(String nameKey, String descriptionKey, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultValue) {
		return Option.<Boolean>createBuilder()
			.name(Component.translatable(nameKey))
			.description(OptionDescription.of(Component.translatable(descriptionKey)))
			.binding(defaultValue, getter, setter)
			.controller(BooleanControllerBuilder::create)
			.build();
	}

	private static Option<Integer> intOption(
		String nameKey,
		String descriptionKey,
		Supplier<Integer> getter,
		Consumer<Integer> setter,
		int defaultValue,
		int min,
		int max
	) {
		return Option.<Integer>createBuilder()
			.name(Component.translatable(nameKey))
			.description(OptionDescription.of(Component.translatable(descriptionKey)))
			.binding(defaultValue, getter, setter)
			.controller(option -> IntegerFieldControllerBuilder.create(option).range(min, max))
			.build();
	}

	private static Option<String> stringOption(
		String nameKey,
		String descriptionKey,
		Supplier<String> getter,
		Consumer<String> setter,
		String defaultValue
	) {
		return Option.<String>createBuilder()
			.name(Component.translatable(nameKey))
			.description(OptionDescription.of(Component.translatable(descriptionKey)))
			.binding(defaultValue, getter, setter)
			.controller(StringControllerBuilder::create)
			.build();
	}

	private static ButtonOption keyButton(
		PeekConfig config,
		String nameKey,
		String descriptionKey,
		String promptKey,
		Supplier<String> getter,
		Consumer<String> setter,
		Consumer<InputConstants.Key> liveApply
	) {
		return ButtonOption.createBuilder()
			.name(Component.translatable(nameKey))
			.text(Component.literal(KeyNameUtil.displayName(getter.get())))
			.description(OptionDescription.of(Component.translatable(descriptionKey)))
			.action((screen, option) -> KeyCaptureScreen.open(
				screen,
				Component.translatable(nameKey),
				Component.translatable(promptKey),
				Component.literal(KeyNameUtil.displayName(getter.get())),
				key -> {
					setter.accept(KeyNameUtil.displayName(key.getName()));
					liveApply.accept(key);
					KeyMapping.resetMapping();
				}
			))
			.build();
	}
}
