package shipwrights.dataplanets.systemCreation.dimension.biome;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.*;
import shipwrights.dataplanets.systemCreation.CelestialData;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.runtimeRegistration.RegistryUtil;

import java.util.ArrayList;
import java.util.List;

import static shipwrights.dataplanets.DataplanetsMod.MOD_ID;

public class BiomeCreator {

    public List<Pair<Climate.ParameterPoint, Holder<Biome>>> createAndRegisterBiomes(SystemCreator.SystemCreationContext context, CelestialData celestialData) {
        int biomeCount = 3 + context.random.nextInt(4);
        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomeList = new ArrayList<>();

        for (int i = 0; i < biomeCount; i++) {
            double variationFactor = (double) i / biomeCount;
            float biomeHeight = 0.0f;

            String biomeName = celestialData.name() + "_biome_" + i;

            Biome biome = BiomeCreator.createBiome(context.random, celestialData, variationFactor, context, biomeName);

            ResourceLocation biomeLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, biomeName);
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeLocation);
            RegistryUtil.registerBiome(context.server, biomeLocation, biome);

            BiomeTags.addTagsToBiome(context, biomeLocation, celestialData, variationFactor);

            // Create climate parameters for this biome based on variation
            Climate.ParameterPoint climateParams = createClimateParameters(celestialData, variationFactor, biomeHeight);

            // Create holder for the biome
            Holder<Biome> biomeHolder = context.server.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getHolderOrThrow(biomeKey);

            biomeList.add(Pair.of(climateParams, biomeHolder));
        }

        return biomeList;
    }

    /**
     * Create climate parameters for a biome based on planet data and variation factor
     */
    private Climate.ParameterPoint createClimateParameters(CelestialData celestialData, double variationFactor, float biomeHeight) {
        // IMPORTANT: The noise router in TerrainGenCreator multiplies noise (-1 to +1) by planet parameters
        // So if planet.temperature() = 1.0, actual temperature noise ranges from -1.0 to +1.0
        // We need to distribute biomes across the ACTUAL noise range, not around the planet parameter

        // Calculate the actual noise ranges produced by TerrainGenCreator
        double tempMultiplier = clampClimateParameter(celestialData.temperature());
        double humidityMultiplier = clampClimateParameter(celestialData.atmosphericDensity());
        double erosionMultiplier = clampClimateParameter(celestialData.weirdness() * 0.8);
        double ridgeMultiplier = clampClimateParameter(celestialData.terrainRoughness() * 0.8);

        // Continentalness is calculated differently - it's based on planet.size() * 0.2 * noise
        double continentalnessMultiplier = celestialData.size() * 0.2;

        // Each biome should occupy a slice of the expected noise range
        // Distribute biomes evenly across [-multiplier, +multiplier]
        float sliceSize = 0.6f; // Smaller slices = less overlap = better distribution

        // Temperature: distribute across the range the noise actually produces
        float tempMin = (float)(tempMultiplier * -1.0 + (variationFactor * 2.0 * tempMultiplier));
        float tempMax = tempMin + sliceSize;

        // Humidity: distribute across atmospheric density range
        float humidityMin = (float)(humidityMultiplier * -1.0 + (variationFactor * 2.0 * humidityMultiplier));
        float humidityMax = humidityMin + sliceSize;

        // Continentalness: distribute across size-based range
        float continentalnessMin = (float)(continentalnessMultiplier * -1.0 + (variationFactor * 2.0 * continentalnessMultiplier));
        float continentalnessMax = continentalnessMin + sliceSize;

        // Erosion: distribute across weirdness-based range
        float erosionMin = (float)(erosionMultiplier * -1.0 + (variationFactor * 2.0 * erosionMultiplier));
        float erosionMax = erosionMin + sliceSize;

        // Depth stays simple
        float depth = biomeHeight;

        // Ridges/weirdness: distribute across terrain roughness range
        float weirdnessMin = (float)(ridgeMultiplier * -1.0 + (variationFactor * 2.0 * ridgeMultiplier));
        float weirdnessMax = weirdnessMin + sliceSize;

        // Create non-overlapping or minimally overlapping ranges for better distribution
        return Climate.parameters(
                Climate.Parameter.span(clampClimate(tempMin), clampClimate(tempMax)),
                Climate.Parameter.span(clampClimate(humidityMin), clampClimate(humidityMax)),
                Climate.Parameter.span(clampClimate(continentalnessMin), clampClimate(continentalnessMax)),
                Climate.Parameter.span(clampClimate(erosionMin), clampClimate(erosionMax)),
                Climate.Parameter.point(depth),
                Climate.Parameter.span(clampClimate(weirdnessMin), clampClimate(weirdnessMax)),
                0L // offset
        );
    }

    /**
     * Clamp a value to the valid climate parameter range [-2.0, 2.0]
     */
    private static double clampClimateParameter(double value) {
        return Math.max(-1.99, Math.min(1.99, value));
    }

    /**
     * Clamp climate parameter values to Minecraft's allowed range
     */
    private static float clampClimate(double value) {
        return (float) Math.max(-2.0, Math.min(2.0, value));
    }

    private static Biome createBiome(RandomSource random, CelestialData celestialData, double variationFactor, SystemCreator.SystemCreationContext context, String biomeName) {
        // Vary temperature based on planet base temperature and variation
        float temperature = (float) (celestialData.temperature() + (variationFactor - 0.5) * 0.4);

        // Vary downfall (precipitation) based on atmospheric density and sea level
        float downfall = (float) ((celestialData.atmosphericDensity() + celestialData.seaLevel()) / 2.0);

        // Determine if this biome has precipitation
        boolean hasPrecipitation = downfall > 0.3 && temperature > 0.15;

        // Create special effects based on planet properties
        BiomeSpecialEffects.Builder effectsBuilder = new BiomeSpecialEffects.Builder()
                .fogColor(deriveFogColor(celestialData, variationFactor))
                .waterColor(deriveWaterColor(celestialData, variationFactor))
                .waterFogColor(deriveWaterFogColor(celestialData, variationFactor))
                .skyColor(deriveSkyColor(celestialData, variationFactor))
                .grassColorOverride(deriveGrassColor(celestialData, variationFactor))
                .foliageColorOverride(deriveFoliageColor(celestialData, variationFactor));

        // Create mob spawn settings (empty for now - no mobs on generated planets)
        MobSpawnSettings mobSpawnSettings = new MobSpawnSettings.Builder().build();

        BiomeGenerationSettings generationSettings = BiomeFeatures.getBiomeGenerationSettings(context, celestialData, variationFactor, biomeName);

        // Build and return the biome
        return new Biome.BiomeBuilder()
                .hasPrecipitation(hasPrecipitation)
                .temperature(temperature)
                .downfall(downfall)
                .specialEffects(effectsBuilder.build())
                .mobSpawnSettings(mobSpawnSettings)
                .generationSettings(generationSettings)
                .build();
    }

    private static int deriveFogColor(CelestialData celestialData, double variationFactor) {
        // Derive fog color based on temperature, atmospheric density, and variation
        double temp = celestialData.temperature();
        double atmosphere = celestialData.atmosphericDensity();

        // Vary the temperature slightly for this biome
        double biomeTemp = temp + (variationFactor - 0.5) * 0.3;

        // Calculate alpha based on atmospheric density (thicker atmosphere = more opaque fog)
        int alpha = Math.min(255, (int)(atmosphere * 127.5) + 64);

        int red, green, blue;

        if (biomeTemp > 1.5) {
            // Hot planets - reddish/orange fog with variation
            red = 255;
            green = 200 - (int)((biomeTemp - 1.5) * 100) + (int)(variationFactor * 30);
            blue = 100 - (int)((biomeTemp - 1.5) * 50) + (int)(variationFactor * 20);
        } else if (biomeTemp < 0.5) {
            // Cold planets - blue/white fog with variation
            red = 150 + (int)((0.5 - biomeTemp) * 200) - (int)(variationFactor * 40);
            green = 180 + (int)((0.5 - biomeTemp) * 150) - (int)(variationFactor * 30);
            blue = 255;
        } else {
            // Earth-like - light blue fog with subtle variation
            red = 0xC0 + (int)(variationFactor * 20) - 10;
            green = 0xD8 + (int)(variationFactor * 15) - 7;
            blue = 0xFF;
        }

        // Clamp values to valid range
        red = Math.max(0, Math.min(255, red));
        green = Math.max(0, Math.min(255, green));
        blue = Math.max(0, Math.min(255, blue));

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int deriveWaterColor(CelestialData celestialData, double variationFactor) {
        // Base water color on primary fluid and temperature
        if (celestialData.temperature() > 1.8) {
            // Lava planets - orange/red water
            return 0xFF6600 + (int)(variationFactor * 0x004400);
        } else if (celestialData.temperature() < 0.4) {
            // Ice planets - light blue/white water
            return 0xAADDFF - (int)(variationFactor * 0x002200);
        } else {
            // Normal water - vary from deep blue to cyan
            return 0x3F76E4 + (int)(variationFactor * 0x204020);
        }
    }

    private static int deriveWaterFogColor(CelestialData celestialData, double variationFactor) {
        // Water fog is generally darker than water color
        int waterColor = deriveWaterColor(celestialData, variationFactor);
        return (waterColor & 0xFEFEFE) >> 1; // Darken by dividing RGB components by 2
    }

    private static int deriveSkyColor(CelestialData celestialData, double variationFactor) {
        double temp = celestialData.temperature();
        double atmosphere = celestialData.atmosphericDensity();

        if (atmosphere < 0.3) {
            // Thin atmosphere - dark/black sky
            return (int)(variationFactor * 0x101010);
        } else if (temp > 2.0) {
            // Very hot - reddish sky
            return 0x8B0000 + (int)(variationFactor * 0x300000);
        } else if (temp > 1.5) {
            // Hot - orange sky
            return 0xFF8C00 - (int)(variationFactor * 0x002000);
        } else if (temp < 0.5) {
            // Cold - pale sky
            return 0xC0D8FF - (int)(variationFactor * 0x001020);
        } else {
            // Earth-like - blue sky
            return 0x78A7FF + (int)(variationFactor * 0x040400);
        }
    }

    private static int deriveGrassColor(CelestialData celestialData, double variationFactor) {
        double temp = celestialData.temperature();
        double weirdness = celestialData.weirdness();

        if (weirdness > 1.5) {
            // Weird planets - purple/pink vegetation
            return 0xAA55AA + (int)(variationFactor * 0x003300);
        } else if (temp > 1.5) {
            // Hot planets - yellow/brown grass
            return 0xBFB755 - (int)(variationFactor * 0x202000);
        } else if (temp < 0.5) {
            // Cold planets - blue-green grass
            return 0x5599AA + (int)(variationFactor * 0x002200);
        } else {
            // Normal - green grass
            return 0x77AB2F + (int)(variationFactor * 0x102010);
        }
    }

    private static int deriveFoliageColor(CelestialData celestialData, double variationFactor) {
        // Foliage is generally similar to grass but slightly different
        int grassColor = deriveGrassColor(celestialData, variationFactor);
        // Shift the color slightly towards darker/more saturated
        return (grassColor & 0xFEFEFE) - 0x101010;
    }
}
