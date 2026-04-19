package dev.peekinside.provider;

import dev.peekinside.api.ContainerProvider;
import dev.peekinside.api.PreviewRequest;
import dev.peekinside.api.PreviewResult;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("null")
public final class PackedChestProvider implements ContainerProvider {
	public static final int PRIORITY = 850;

	private static final String MOD_NAMESPACE = "packed-chest";
	private static final String PRIMARY_STATE_KEY = "PrimaryState";
	private static final String SECONDARY_STATE_KEY = "SecondaryState";
	private static final String SECONDARY_BLOCK_ENTITY_KEY = "SecondaryBlockEntity";
	private static final String SECONDARY_BLOCK_ENTITY_TYPE_KEY = "SecondaryBlockEntityType";
	private static final int COLUMNS = 9;

	@Override
	public @Nullable PreviewResult provide(PreviewRequest request) {
		Identifier itemId = BuiltInRegistries.ITEM.getKey(request.stack().getItem());
		if (!MOD_NAMESPACE.equals(itemId.getNamespace()) || request.player() == null) {
			return null;
		}

		TypedEntityData<BlockEntityType<?>> primaryData = request.stack().get(DataComponents.BLOCK_ENTITY_DATA);
		if (primaryData == null) {
			return null;
		}

		HolderLookup.Provider registries = request.player().level().registryAccess();
		CompoundTag packedData = copyCustomData(request.stack());
		List<ItemStack> slots = new ArrayList<>();
		Component label = appendContainer(primaryData, readState(packedData, PRIMARY_STATE_KEY, defaultState(primaryData.type())), registries, slots, null);

		CompoundTag secondaryBlockEntity = packedData.getCompoundOrEmpty(SECONDARY_BLOCK_ENTITY_KEY);
		if (!secondaryBlockEntity.isEmpty()) {
			BlockEntityType<?> secondaryType = readSecondaryType(packedData);
			if (secondaryType != null) {
				label = appendContainer(
					TypedEntityData.of(secondaryType, secondaryBlockEntity),
					readState(packedData, SECONDARY_STATE_KEY, defaultState(secondaryType)),
					registries,
					slots,
					label
				);
			}
		}

		if (slots.stream().allMatch(ItemStack::isEmpty)) {
			return null;
		}

		int rows = Math.max(1, (slots.size() + COLUMNS - 1) / COLUMNS);
		List<ItemStack> normalized = new ArrayList<>(COLUMNS * rows);
		for (int index = 0; index < COLUMNS * rows; index++) {
			normalized.add(index < slots.size() ? slots.get(index) : ItemStack.EMPTY);
		}

		return new PreviewResult(List.copyOf(normalized), COLUMNS, rows, label);
	}

	private static Component appendContainer(
		TypedEntityData<BlockEntityType<?>> entityData,
		BlockState state,
		HolderLookup.Provider registries,
		List<ItemStack> slots,
		@Nullable Component label
	) {
		BlockEntity blockEntity = entityData.type().create(BlockPos.ZERO, state);
		if (blockEntity == null || !entityData.loadInto(blockEntity, registries) || !(blockEntity instanceof Container container)) {
			return label;
		}

		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			slots.add(container.getItem(slot).copy());
		}

		if (label == null && blockEntity instanceof BaseContainerBlockEntity namedContainer) {
			return namedContainer.getDisplayName();
		}

		return label;
	}

	private static CompoundTag copyCustomData(ItemStack stack) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		return customData == null ? new CompoundTag() : customData.copyTag();
	}

	private static BlockState readState(CompoundTag packedData, String key, BlockState fallback) {
		CompoundTag stateTag = packedData.getCompoundOrEmpty(key);
		if (stateTag.isEmpty()) {
			return fallback;
		}

		BlockState state = NbtUtils.readBlockState(BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK), stateTag);
		return state.isAir() ? fallback : state;
	}

	private static @Nullable BlockEntityType<?> readSecondaryType(CompoundTag packedData) {
		Identifier id = Identifier.tryParse(packedData.getStringOr(SECONDARY_BLOCK_ENTITY_TYPE_KEY, ""));
		return id == null ? null : BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(id).orElse(null);
	}

	private static BlockState defaultState(BlockEntityType<?> type) {
		if (type == BlockEntityType.BARREL) {
			return Blocks.BARREL.defaultBlockState();
		}

		return Blocks.CHEST.defaultBlockState();
	}
}
