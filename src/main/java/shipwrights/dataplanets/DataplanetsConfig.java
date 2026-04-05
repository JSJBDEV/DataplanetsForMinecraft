package shipwrights.dataplanets;

import net.minecraftforge.common.ForgeConfigSpec;

public class DataplanetsConfig {
    private static ForgeConfigSpec.ConfigValue<Integer> mainSystemPlanets;
    private static ForgeConfigSpec.ConfigValue<Integer> mainSystemPlanetMoonsMax;
    private static ForgeConfigSpec.ConfigValue<Integer> totalAdditionalSystems;
    private static ForgeConfigSpec.ConfigValue<Integer> maxPlanetsInAdditionalSystems;
    private static ForgeConfigSpec.ConfigValue<Integer> maxMoonsForPlanetsInAdditionalSystems;

    public static final ForgeConfigSpec CONFIG_SPEC = buildConfig();

    public static int getMainSystemPlanets() {
        return mainSystemPlanets.get();
    }

    public static int getMainSystemPlanetMoonsMax() {
        return mainSystemPlanetMoonsMax.get();
    }

    public static int getTotalAdditionalSystems() {
        return totalAdditionalSystems.get();
    }

    public static int getMaxMoonsForPlanetsInAdditionalSystems() {
        return maxMoonsForPlanetsInAdditionalSystems.get();
    }

    public static int getMaxPlanetsInAdditionalSystems() {
        return maxPlanetsInAdditionalSystems.get();
    }

    private static ForgeConfigSpec buildConfig() {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        mainSystemPlanets = builder.define("mainSystemPlanets",3);
        mainSystemPlanetMoonsMax = builder.define("mainSystemPlanetMoonsMax",1);
        totalAdditionalSystems = builder.define("totalAdditionalSystems",1);
        maxPlanetsInAdditionalSystems = builder.define("maxPlanetsInAdditionalSystems",5);
        maxMoonsForPlanetsInAdditionalSystems = builder.define("maxMoonsForPlanetsInAdditionalSystems",1);
        return builder.build();
    }
}
