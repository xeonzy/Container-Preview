package dev.peekinside.network;

import dev.peekinside.PeekConstants;

import dev.peekinside.PeekInside;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("null")
public record EnderChestContentsPayload(List<ItemStack> slots) implements CustomPacketPayload {
	public static final Type<EnderChestContentsPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(PeekConstants.MOD_ID, "ec_contents")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, EnderChestContentsPayload> STREAM_CODEC = StreamCodec.composite(
		ItemStack.OPTIONAL_LIST_STREAM_CODEC,
		EnderChestContentsPayload::slots,
		EnderChestContentsPayload::new
	);

	public EnderChestContentsPayload {
		slots = List.copyOf(slots);
	}

	@Override
	public Type<EnderChestContentsPayload> type() {
		return TYPE;
	}
}
