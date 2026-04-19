package dev.peekinside.provider;

import com.mojang.serialization.DynamicOps;
import dev.peekinside.api.ContainerProvider;
import dev.peekinside.api.PreviewRequest;
import dev.peekinside.api.PreviewResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@SuppressWarnings("null")
public final class TagContainerProvider implements ContainerProvider {
	public static final int PRIORITY = 1100;
	private static final int MAX_DEPTH = 6;

	@Override
	public @Nullable PreviewResult provide(PreviewRequest request) {
		if (request.player() == null) {
			return null;
		}

		HolderLookup.Provider registries = request.player().level().registryAccess();
		Tag serialized = ItemStack.CODEC.encodeStart(nbtOps(registries), request.stack()).result().orElse(null);
		if (!(serialized instanceof CompoundTag root)) {
			return null;
		}

		Candidate best = findBestCandidate(registries, root, 0, "root");
		if (best == null || best.nonEmptySlots() == 0) {
			return null;
		}

		int columns = GridShapeRegistry.INSTANCE.getCols(request.stack().getItem(), best.slotCount());
		int rows = GridShapeRegistry.INSTANCE.getRows(request.stack().getItem(), best.slotCount(), columns);
		int requiredSize = columns * rows;
		List<ItemStack> normalized = new ArrayList<>(requiredSize);
		for (int index = 0; index < requiredSize; index++) {
			normalized.add(index < best.slots().size() ? best.slots().get(index).copy() : ItemStack.EMPTY);
		}

		return new PreviewResult(List.copyOf(normalized), columns, rows, null);
	}

	private static @Nullable Candidate findBestCandidate(HolderLookup.Provider registries, CompoundTag root, int depth, String path) {
		if (depth > MAX_DEPTH) {
			return null;
		}

		Candidate best = null;
		for (String key : root.keySet()) {
			Tag tag = root.get(key);
			if (tag == null) {
				continue;
			}

			String childPath = path + "." + key;
			Candidate candidate = switch (tag) {
				case CompoundTag compound -> findBestCandidate(registries, compound, depth + 1, childPath);
				case ListTag list -> bestOf(fromList(registries, list, depth, childPath), scanChildren(registries, list, depth, childPath));
				default -> null;
			};
			best = bestOf(best, candidate);
		}

		return best;
	}

	private static @Nullable Candidate scanChildren(HolderLookup.Provider registries, ListTag list, int depth, String path) {
		if (depth > MAX_DEPTH) {
			return null;
		}

		Candidate best = null;
		for (int index = 0; index < list.size(); index++) {
			Tag child = list.get(index);
			if (child instanceof CompoundTag compound) {
				best = bestOf(best, findBestCandidate(registries, compound, depth + 1, path + "[" + index + "]"));
			}
		}

		return best;
	}

	private static @Nullable Candidate fromList(HolderLookup.Provider registries, ListTag list, int depth, String path) {
		if (list.isEmpty()) {
			return null;
		}

		List<ItemStack> slots = new ArrayList<>();
		int nonEmptySlots = 0;
		int explicitSlotCount = 0;
		for (int index = 0; index < list.size(); index++) {
			Tag element = list.get(index);
			if (!(element instanceof CompoundTag compound)) {
				continue;
			}

			ItemStack stack = parseStack(registries, compound);
			if (stack.isEmpty()) {
				continue;
			}

			int slot = extractSlotIndex(compound);
			if (slot >= 0) {
				explicitSlotCount++;
			} else {
				slot = index;
			}

			while (slots.size() <= slot) {
				slots.add(ItemStack.EMPTY);
			}

			if (!slots.get(slot).isEmpty()) {
				slot = slots.size();
				slots.add(stack.copy());
			} else {
				slots.set(slot, stack.copy());
			}
			nonEmptySlots++;
		}

		if (nonEmptySlots == 0) {
			return null;
		}

		int score = nonEmptySlots * 10 + explicitSlotCount * 4 + preferredPathBonus(path) - depth * 2;
		return new Candidate(List.copyOf(slots), Math.max(slots.size(), nonEmptySlots), nonEmptySlots, score);
	}

	private static ItemStack parseStack(HolderLookup.Provider registries, CompoundTag compound) {
		ItemStack direct = ItemStack.CODEC.parse(nbtOps(registries), compound).result().orElse(ItemStack.EMPTY);
		if (!direct.isEmpty()) {
			return direct;
		}

		// Fallback for legacy format (pre-1.20.5)
		if (compound.contains("id")) {
			try {
				return ItemStack.CODEC.parse(nbtOps(registries), compound).result().orElse(ItemStack.EMPTY);
			} catch (Exception ignored) {
			}
		}

		for (String key : compound.keySet()) {
			Tag child = compound.get(key);
			if (child instanceof CompoundTag nested) {
				ItemStack nestedStack = ItemStack.CODEC.parse(nbtOps(registries), nested).result().orElse(ItemStack.EMPTY);
				if (!nestedStack.isEmpty()) {
					return nestedStack;
				}
			}
		}

		return ItemStack.EMPTY;
	}

	private static int extractSlotIndex(CompoundTag compound) {
		for (String key : List.of("Slot", "slot", "Index", "index")) {
			Tag tag = compound.get(key);
			if (tag instanceof NumericTag numeric) {
				int slot = numeric.intValue();
				if (slot >= 0 && slot < 256) {
					return slot;
				}
			}
		}

		return -1;
	}

	private static int preferredPathBonus(String path) {
		String lowered = path.toLowerCase(Locale.ROOT);
		int bonus = 0;
		for (String token : List.of("item", "items", "inventory", "contents", "container", "slots", "storage", "bag", "backpack", "backpack_items", "main_inventory")) {
			if (lowered.contains(token)) {
				bonus += 5;
			}
		}
		return bonus;
	}


	private static @Nullable Candidate bestOf(@Nullable Candidate first, @Nullable Candidate second) {
		if (first == null) {
			return second;
		}
		if (second == null) {
			return first;
		}

		return second.score() > first.score() ? second : first;
	}

	private record Candidate(List<ItemStack> slots, int slotCount, int nonEmptySlots, int score) {
	}

	private static DynamicOps<Tag> nbtOps(HolderLookup.Provider registries) {
		return RegistryOps.create(NbtOps.INSTANCE, registries);
	}
}
