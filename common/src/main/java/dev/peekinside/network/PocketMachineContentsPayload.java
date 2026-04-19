package dev.peekinside.network;

import dev.peekinside.PeekConstants;

import dev.peekinside.PeekInside;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public record PocketMachineContentsPayload(
	String machineKey,
	String stackId,
	String label,
	int columns,
	int rows,
	List<ItemStack> slots
) implements CustomPacketPayload {
	public static final Type<PocketMachineContentsPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(PeekConstants.MOD_ID, "pm_contents")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, PocketMachineContentsPayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8,
		PocketMachineContentsPayload::machineKey,
		ByteBufCodecs.STRING_UTF8,
		PocketMachineContentsPayload::stackId,
		ByteBufCodecs.STRING_UTF8,
		PocketMachineContentsPayload::label,
		ByteBufCodecs.VAR_INT,
		PocketMachineContentsPayload::columns,
		ByteBufCodecs.VAR_INT,
		PocketMachineContentsPayload::rows,
		ItemStack.OPTIONAL_LIST_STREAM_CODEC,
		PocketMachineContentsPayload::slots,
		PocketMachineContentsPayload::new
	);

	public PocketMachineContentsPayload {
		slots = List.copyOf(slots);
	}

	@Override
	public Type<PocketMachineContentsPayload> type() {
		return TYPE;
	}
}
