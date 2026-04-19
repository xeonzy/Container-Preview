package dev.peekinside;

import dev.peekinside.config.PeekConfig;
import org.slf4j.Logger;

public final class PeekInside {
    public static final String MOD_ID = PeekConstants.MOD_ID;
    public static final Logger LOGGER = PeekConstants.LOGGER;

    private PeekInside() {
    }

    public static void init() {
        PeekConfig.INSTANCE.load();
        PeekKeys.initialize();
        registerCommonProviders();
    }

    private static void registerCommonProviders() {
        var registry = dev.peekinside.api.ContainerProviderRegistry.INSTANCE;
        registry.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "blankpack_component"), dev.peekinside.provider.BlankPackProvider.PRIORITY, new dev.peekinside.provider.BlankPackProvider());
        registry.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "packed_chest"), dev.peekinside.provider.PackedChestProvider.PRIORITY, new dev.peekinside.provider.PackedChestProvider());
        registry.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "bundle_contents"), dev.peekinside.provider.BundleContentsProvider.PRIORITY, new dev.peekinside.provider.BundleContentsProvider());
        registry.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "component_container"), dev.peekinside.provider.ComponentContainerProvider.PRIORITY, new dev.peekinside.provider.ComponentContainerProvider());
        registry.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "tag_container"), dev.peekinside.provider.TagContainerProvider.PRIORITY, new dev.peekinside.provider.TagContainerProvider());
        registry.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "ender_chest"), dev.peekinside.provider.EnderChestProvider.PRIORITY, new dev.peekinside.provider.EnderChestProvider());
        registry.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, "pocket_machines"), dev.peekinside.provider.PocketMachinesProvider.PRIORITY, new dev.peekinside.provider.PocketMachinesProvider());
    }
}
