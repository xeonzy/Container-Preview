package dev.peekinside.mixin;

import dev.peekinside.api.EnderChestContainerAccess;
import dev.peekinside.network.EnderChestContentsPayload;
import dev.peekinside.Services;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.jspecify.annotations.Nullable;

@Mixin(PlayerEnderChestContainer.class)
@SuppressWarnings("null")
public abstract class PlayerEnderChestContainerMixin implements EnderChestContainerAccess {
	@Unique
	private @Nullable Player peekinside$owner;
	@Unique
	private long peekinside$lastSignature = Long.MIN_VALUE;

	@Override
	public void peekinside$setOwner(Player owner) {
		if (this.peekinside$owner != owner) {
			this.peekinside$owner = owner;
			this.peekinside$lastSignature = Long.MIN_VALUE;
		}
	}

	@Inject(method = "fromSlots", at = @At("TAIL"))
	private void peekinside$syncOnLoad(ValueInput.TypedInputList<ItemStackWithSlot> slots, CallbackInfo ci) {
		peekinside$syncToClient();
	}

	public void peekinside$syncToClient() {
		if (!(this.peekinside$owner instanceof ServerPlayer serverPlayer)) {
			return;
		}

		PlayerEnderChestContainer container = (PlayerEnderChestContainer) (Object) this;
		List<ItemStack> slots = new ArrayList<>(container.getContainerSize());
		long signature = 1125899906842597L;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			ItemStack stack = container.getItem(slot).copy();
			slots.add(stack);
			signature = signature * 31L + ItemStack.hashItemAndComponents(stack);
		}

		if (signature == this.peekinside$lastSignature) {
			return;
		}

		this.peekinside$lastSignature = signature;
		Services.PLATFORM.sendToPlayer(serverPlayer, new EnderChestContentsPayload(slots));
	}
}
