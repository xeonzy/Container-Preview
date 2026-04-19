package dev.peekinside.forge;

import dev.peekinside.PeekInside;
import dev.peekinside.PeekConstants;
import dev.peekinside.PeekKeys;
import dev.peekinside.config.PeekFallbackConfigScreen;
import dev.peekinside.config.YaclScreenProvider;
import dev.peekinside.render.PreviewTooltipComponent;
import dev.peekinside.render.PreviewTooltipData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(PeekConstants.MOD_ID)
public class PeekInsideForge {

    public PeekInsideForge(FMLJavaModLoadingContext context) {
        PeekInside.LOGGER.info("Initializing PeekInside for Forge");
        PeekInside.init();
        var busGroup = context.getModBusGroup();
        RegisterKeyMappingsEvent.getBus(busGroup).addListener(this::onRegisterKeyMappings);
        RegisterClientTooltipComponentFactoriesEvent.getBus(busGroup).addListener(this::onRegisterTooltipComponents);
        FMLLoadCompleteEvent.getBus(busGroup).addListener(this::onLoadComplete);
        ForgeNetworking.init();
        PeekInside.LOGGER.info("Registered listeners for KeyMappings, TooltipComponents, Payloads, and Setup");

        @SuppressWarnings("removal")
        var mlc = ModLoadingContext.get();
        mlc.registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> {
                if (YaclScreenProvider.isAvailable()) {
                    return YaclScreenProvider.create(parent);
                }
                return PeekFallbackConfigScreen.create(parent);
            })
        );
    }

    private void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(dev.peekinside.RuntimeContainerCompat::register);
    }

    private void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(PeekKeys.PREVIEW);
        event.register(PeekKeys.DETAIL);
        event.register(PeekKeys.LOCK);
    }

    private void onRegisterTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(PreviewTooltipData.class, PreviewTooltipComponent::new);
    }
}
