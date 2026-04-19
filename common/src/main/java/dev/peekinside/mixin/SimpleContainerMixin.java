package dev.peekinside.mixin;

import dev.peekinside.api.EnderChestContainerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.SimpleContainer.class)
public abstract class SimpleContainerMixin {
	@Inject(method = "setChanged", at = @At("TAIL"))
	private void peekinside$syncOnChange(CallbackInfo ci) {
		peekinside$syncIfEnderChest();
	}

	@Inject(method = "clearContent", at = @At("TAIL"))
	private void peekinside$syncOnClear(CallbackInfo ci) {
		peekinside$syncIfEnderChest();
	}

	private void peekinside$syncIfEnderChest() {
		if (this instanceof EnderChestContainerAccess access) {
			access.peekinside$syncToClient();
		}
	}
}
