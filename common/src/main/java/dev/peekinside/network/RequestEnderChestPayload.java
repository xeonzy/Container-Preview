package dev.peekinside.network;

import dev.peekinside.PeekConstants;

import dev.peekinside.PeekInside;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@SuppressWarnings("null")
public record RequestEnderChestPayload() implements CustomPacketPayload {
	public static final RequestEnderChestPayload INSTANCE = new RequestEnderChestPayload();
	public static final Type<RequestEnderChestPayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(PeekConstants.MOD_ID, "ec_request")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestEnderChestPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<RequestEnderChestPayload> type() {
		return TYPE;
	}
}
