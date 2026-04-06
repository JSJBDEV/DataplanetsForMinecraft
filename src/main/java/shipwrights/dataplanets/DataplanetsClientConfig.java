package shipwrights.dataplanets;

import net.minecraftforge.common.ForgeConfigSpec;

public class DataplanetsClientConfig {
    private static ForgeConfigSpec.BooleanValue exportTextures;

    public static final ForgeConfigSpec CONFIG_SPEC = buildConfig();

    private static ForgeConfigSpec buildConfig() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        exportTextures = builder
                .comment("When true, generated planet textures are saved as PNG files in config/dataplanets/textures/")
                .define("exportTextures", false);
        return builder.build();
    }

    public static boolean shouldExportTextures() {
        return exportTextures.get();
    }
}
