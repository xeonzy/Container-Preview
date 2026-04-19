package dev.peekinside.network;

import dev.peekinside.PeekConstants;

import dev.peekinside.PeekInside;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@SuppressWarnings("null")
public record RequestPocketMachinePayload(String machineKey, String stackId) implements CustomPacketPayload {
	public static final Type<RequestPocketMachinePayload> TYPE = new Type<>(
		Identifier.fromNamespaceAndPath(PeekConstants.MOD_ID, "pm_request")
	);
	public static final StreamCodec<RegistryFriendlyByteBuf, RequestPocketMachinePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.STRING_UTF8,
		RequestPocketMachinePayload::machineKey,
		ByteBufCodecs.STRING_UTF8,
		RequestPocketMachinePayload::stackId,
		RequestPocketMachinePayload::new
	);

	@Override
	public Type<RequestPocketMachinePayload> type() {
		return TYPE;
	}
}
