package dev.peekinside.provider;

import dev.peekinside.PeekInside;
import dev.peekinside.Services;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("null")
public final class PocketMachinesCompat {
	public static final String MOD_ID = "pocketmachines";

	private static final String PERSISTED_MACHINES_CLASS = "com.hypherionmc.pocketmachines.common.world.PersistedMachines";
	private static final Map<String, MachineType> MACHINE_TYPES = Map.of(
		"pocket_chest", new MachineType("pocket_chest", "TG_CHEST", "POCKET_CHEST"),
		"pocket_furnace", new MachineType("pocket_furnace", "TG_FURNACE", "POCKET_FURNACE"),
		"pocket_blast_furnace", new MachineType("pocket_blast_furnace", "TG_BLAST_FURNACE", "POCKET_BLAST_FURNACE"),
		"pocket_brewing_stand", new MachineType("pocket_brewing_stand", "TG_BREWING_STAND", "POCKET_BREWING_STAND"),
		"pocket_smoker", new MachineType("pocket_smoker", "TG_SMOKER", "POCKET_SMOKER")
	);

	private PocketMachinesCompat() {
	}

	public static @Nullable MachineType machineType(String itemPath) {
		return MACHINE_TYPES.get(itemPath);
	}

	public static @Nullable String stackId(ItemStack stack, MachineType machineType) {
		CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
		if (customData == null) {
			return null;
		}

		String stackId = customData.copyTag().getStringOr(machineType.storageKey(), "");
		return stackId.isBlank() ? null : stackId;
	}

	public static @Nullable ResolvedPreview resolvePreview(MachineType machineType, String stackId, ServerPlayer player) {
		if (!Services.PLATFORM.isModLoaded(MOD_ID)) {
			return null;
		}

		try {
			Object saveHolder = saveHolder(machineType);
			for (Object saveItem : saveItems(saveHolder)) {
				if (!stackId.equals(invokeString(saveItem, "getStackId")) || !player.getUUID().toString().equals(invokeString(saveItem, "getUserId"))) {
					continue;
				}

				Object value = invoke(saveItem, "getValue");
				if (!(value instanceof Container container)) {
					return null;
				}

				List<ItemStack> slots = new ArrayList<>(container.getContainerSize());
				for (int slot = 0; slot < container.getContainerSize(); slot++) {
					slots.add(container.getItem(slot).copy());
				}

				int columns = guessColumns(container.getContainerSize());
				int rows = Math.max(1, (container.getContainerSize() + columns - 1) / columns);
				Component label = value instanceof Nameable nameable ? nameable.getDisplayName() : null;
				return new ResolvedPreview(List.copyOf(slots), columns, rows, label);
			}
		} catch (ReflectiveOperationException exception) {
			PeekInside.LOGGER.warn("Pocket Machines compat lookup failed for {}", stackId, exception);
		}

		return null;
	}

	private static Object saveHolder(MachineType machineType) throws ReflectiveOperationException {
		Class<?> persistedMachinesClass = Class.forName(PERSISTED_MACHINES_CLASS);
		Field saveHolderField = persistedMachinesClass.getField(machineType.saveHolderField());
		return saveHolderField.get(null);
	}

	private static List<?> saveItems(Object saveHolder) throws ReflectiveOperationException {
		Object items = invoke(saveHolder, "getItems");
		return items instanceof List<?> list ? list : List.of();
	}

	private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
		Method method = target.getClass().getMethod(methodName);
		return method.invoke(target);
	}

	private static String invokeString(Object target, String methodName) throws ReflectiveOperationException {
		Object value = invoke(target, methodName);
		return value instanceof String string ? string : "";
	}

	private static int guessColumns(int size) {
		if (size == 5) {
			return 5;
		}
		if (size <= 3) {
			return 3;
		}
		if (size % 9 == 0) {
			return 9;
		}
		return Math.min(9, Math.max(1, size));
	}

	public record MachineType(String itemPath, String storageKey, String saveHolderField) {
	}

	public record ResolvedPreview(List<ItemStack> slots, int columns, int rows, @Nullable Component label) {
	}
}
