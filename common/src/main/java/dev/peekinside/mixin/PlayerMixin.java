package dev.peekinside.mixin;

import dev.peekinside.api.EnderChestContainerAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
	@Shadow
	protected PlayerEnderChestContainer enderChestInventory;

	@Inject(method = "getEnderChestInventory", at = @At("RETURN"))
	private void peekinside$attachOwner(CallbackInfoReturnable<PlayerEnderChestContainer> cir) {
		peekinside$attachEnderChestOwner();
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void peekinside$tick(CallbackInfo ci) {
		peekinside$attachEnderChestOwner();
	}

	@Unique
	private void peekinside$attachEnderChestOwner() {
		if (this.enderChestInventory instanceof EnderChestContainerAccess access) {
			access.peekinside$setOwner((Player) (Object) this);
		}
	}
}
