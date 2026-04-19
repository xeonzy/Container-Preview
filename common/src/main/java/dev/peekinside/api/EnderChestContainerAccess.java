package dev.peekinside.api;

import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public interface EnderChestContainerAccess {
	void peekinside$setOwner(@Nullable Player owner);
	void peekinside$syncToClient();
}
