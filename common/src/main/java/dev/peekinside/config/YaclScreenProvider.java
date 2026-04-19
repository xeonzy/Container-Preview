package dev.peekinside.config;

import net.minecraft.client.gui.screens.Screen;

/**
 * Isolated class to prevent NoClassDefFoundError when YACL is missing.
 */
public final class YaclScreenProvider {
    private static final String YACL_SCREEN_CLASS = "dev.isxander.yacl3.api.YetAnotherConfigLib";

    private YaclScreenProvider() {
    }

    public static boolean isAvailable() {
        try {
            Class.forName(YACL_SCREEN_CLASS, false, YaclScreenProvider.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public static Screen create(Screen parent) {
        return PeekYaclConfigScreen.create(parent);
    }
}
