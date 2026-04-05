package shipwrights.dataplanets.runtimeRegistration;

import com.google.gson.JsonElement;
import com.mojang.serialization.Lifecycle;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.LevelResource;
import shipwrights.dataplanets.DataplanetsConfig;
import shipwrights.dataplanets.DPPackets;
import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.PlanetLookup;
import shipwrights.dataplanets.mixin.MinecraftServerAccessor;
import shipwrights.dataplanets.systemCreation.CelestialSource;
import shipwrights.dataplanets.systemCreation.StarData;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RegistryUtil {

    public static void registerBiome(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            Biome biome
    ) {
        registerThing(server, Registries.BIOME, ResourceKey.create(Registries.BIOME, resourceLocation), biome);
        writeToDatapack(server, resourceLocation, "worldgen/biome", Biome.DIRECT_CODEC, biome);
    }

    public static void addBiomeToTag(MinecraftServer server, ResourceLocation biomeLocation, TagKey<Biome> tag) {
        addBiomeToTag(server, tag, biomeLocation);
    }

    public static void registerDimensionType(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            DimensionType dimensionType
    ) {
        registerThing(server, Registries.DIMENSION_TYPE, ResourceKey.create(Registries.DIMENSION_TYPE, resourceLocation), dimensionType);
        writeToDatapack(server, resourceLocation, "dimension_type", DimensionType.DIRECT_CODEC, dimensionType);
    }

    public static void registerLevelStem(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            LevelStem levelStem,
            ServerPhase serverPhase) {
        if (serverPhase == ServerPhase.starting) {
            registerThing(server, Registries.LEVEL_STEM, ResourceKey.create(Registries.LEVEL_STEM, resourceLocation), levelStem);
        } else {
            DimensionManager.INSTANCE.queueLevelForRegistration(ResourceKey.create(Registries.DIMENSION, resourceLocation), levelStem);
        }
        writeToDatapack(server, resourceLocation, "dimension", LevelStem.CODEC, levelStem);
    }

    public static void registerConfiguredCarver(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            ConfiguredWorldCarver<?> configuredCarver
    ) {
        registerThing(server, Registries.CONFIGURED_CARVER, ResourceKey.create(Registries.CONFIGURED_CARVER, resourceLocation), configuredCarver);
        //TODO write to datapack

    }

    public static void registerPlacedFeature(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            PlacedFeature placedFeature
    ) {
        registerThing(server, Registries.PLACED_FEATURE, ResourceKey.create(Registries.PLACED_FEATURE, resourceLocation), placedFeature);
        writeToDatapack(server, resourceLocation, "worldgen/placed_feature", PlacedFeature.DIRECT_CODEC, placedFeature);

    }

    public static void registerConfiguredFeature(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            ConfiguredFeature<?, ?> configuredFeature
    ) {
        registerThing(server, Registries.CONFIGURED_FEATURE, ResourceKey.create(Registries.CONFIGURED_FEATURE, resourceLocation), configuredFeature);
        writeToDatapack(server, resourceLocation, "worldgen/configured_feature", ConfiguredFeature.DIRECT_CODEC, configuredFeature);

    }

    public static void registerNoise(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            NormalNoise.NoiseParameters noiseParameters
    ) {
        registerThing(server, Registries.NOISE, ResourceKey.create(Registries.NOISE, resourceLocation), noiseParameters);
        //TODO write to datapack

    }

    public static void registerNoiseSettings(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            NoiseGeneratorSettings noiseGeneratorSettings
    ) {
        registerThing(server, Registries.NOISE_SETTINGS, ResourceKey.create(Registries.NOISE_SETTINGS, resourceLocation), noiseGeneratorSettings);
        writeToDatapack(server, resourceLocation, "worldgen/noise_settings", NoiseGeneratorSettings.DIRECT_CODEC, noiseGeneratorSettings);
    }

    public static void registerGenesisFiles(MinecraftServer server, Celestial celestial, ResourceLocation celestialRL,boolean sendTextures)
    {
       registerThing(server, GenesisMod.CELESTIALS_KEY,ResourceKey.create(GenesisMod.CELESTIALS_KEY,celestialRL),celestial);
       writeToDatapack(server,celestialRL,"genesis/celestials",Celestial.CODEC,celestial);
       if(sendTextures) sendTexturesToClient(celestialRL);
    }

    public static void sendTexturesToClient(ResourceLocation celestialRL)
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("name",celestialRL.getPath());

        tag.put("state", NbtUtils.writeBlockState(BuiltInRegistries.BLOCK.get(PlanetLookup.get(celestialRL).primaryBlock()).defaultBlockState()));

        PlanetTexturerPacket packet = new PlanetTexturerPacket(tag);
        DPPackets.sendToAll(DPPackets.INSTANCE,packet);
    }

    @SuppressWarnings("deprecation")
    private static <T> void registerThing(
            MinecraftServer server,
            ResourceKey<Registry<T>> registry,
            ResourceKey<T> keyToRegister,
            T thingToRegister
    ) {
        Registry<T> reg = server.registryAccess().registryOrThrow(registry);
        if (reg instanceof MappedRegistry<T> mapped) {
            mapped.unfreeze();
            mapped.register(keyToRegister, thingToRegister, Lifecycle.experimental());
            mapped.freeze();
        } else {
            throw new IllegalArgumentException("Cannot modify registry: " + registry.registry());
        }
    }

    public static boolean setupDatapackFolder(MinecraftServer server) {
        Path basePath = ((MinecraftServerAccessor) server).getStorageSource().getLevelPath(LevelResource.DATAPACK_DIR);
        Path dataplanetsFolder = basePath.resolve("dataplanets-generated");

        String mcMeta = "{\n" +
                "  \"pack\": {\n" +
                "    \"description\": \"Dataplanets Generated\",\n" +
                "    \"forge:server_data_pack_format\": 15,\n" +
                "    \"pack_format\": 15\n" +
                "  }\n" +
                "}";

        boolean newFolderCreated = false;

        try {
            if (!Files.exists(dataplanetsFolder)) {
                Files.createDirectories(dataplanetsFolder);
                newFolderCreated = true;

                if(DataplanetsConfig.getMainSystemPlanets()>0)
                {
                    SystemCreator creator = new SystemCreator();
                    SystemCreator.SystemCreationContext context = new SystemCreator.SystemCreationContext(server, true, ServerPhase.running);
                    for (int i = 0; i < DataplanetsConfig.getMainSystemPlanets(); i++) {
                        CelestialSource source = CelestialSource.createRandomPlanet(context.nextPlanetName(), context.random);
                        creator.createBody(source, context);
                        PlanetLookup.store(context.server, source);

                        for (int j = 0; j < context.random.nextInt(DataplanetsConfig.getMainSystemPlanetMoonsMax()); j++) {
                            CelestialSource moonSource = CelestialSource.createRandomMoon(context.currentPlanetName()+"_"+j,context.random);
                            creator.createBody(moonSource,context,"dataplanets:"+source.name());
                            PlanetLookup.store(context.server, moonSource);
                        }

                    }
                }

                if(DataplanetsConfig.getTotalAdditionalSystems()>0)
                {
                    for (int i = 0; i < DataplanetsConfig.getTotalAdditionalSystems(); i++) {
                        SystemCreator creator = new SystemCreator();
                        SystemCreator.SystemCreationContext context = new SystemCreator.SystemCreationContext(server, true, ServerPhase.running);
                        SystemCreator.registerStaticToGenesis(StarData.createRandom(context),context);

                        for (int j = 0; j < context.random.nextInt(DataplanetsConfig.getMaxPlanetsInAdditionalSystems()); j++) {
                            CelestialSource source = CelestialSource.createRandomPlanet(context.nextPlanetName(), context.random);
                            creator.createBody(source, context,"dataplanets:"+context.systemName);
                            PlanetLookup.store(context.server, source);

                            for (int k = 0; k < context.random.nextInt(DataplanetsConfig.getMaxMoonsForPlanetsInAdditionalSystems()); k++) {
                                CelestialSource moonSource = CelestialSource.createRandomMoon(context.currentPlanetName()+"_"+k,context.random);
                                creator.createBody(moonSource,context,"dataplanets:"+source.name());
                                PlanetLookup.store(context.server, moonSource);
                            }
                        }

                    }
                }

            }

            Path mcMetaPath = dataplanetsFolder.resolve("pack.mcmeta");
            if (!Files.exists(mcMetaPath)) {
                Files.writeString(mcMetaPath, mcMeta);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create dataplanets-generated folder", e);
        }

        return newFolderCreated;
    }

    public static <T> void writeToDatapack(
            MinecraftServer server,
            ResourceLocation resourceLocation,
            String path,
            com.mojang.serialization.Codec<T> codec,
            T object
    ) {
        Path basePath = ((MinecraftServerAccessor) server).getStorageSource().getLevelPath(LevelResource.DATAPACK_DIR);
        Path dataplanetsFolder = basePath.resolve("dataplanets-generated");
        Path typeFolder = dataplanetsFolder.resolve("data")
                .resolve(resourceLocation.getNamespace())
                .resolve(path);
        Path objectFile = typeFolder.resolve(resourceLocation.getPath() + ".json");

        try {
            Files.createDirectories(typeFolder);

            // Use RegistryOps to serialize with registry references instead of inlining
            RegistryAccess registryAccess = server.registryAccess();
            RegistryOps<JsonElement> registryOps = RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registryAccess);

            com.mojang.serialization.DataResult<JsonElement> result = codec.encodeStart(
                    registryOps,
                    object
            );

            // Check if encoding was successful
            if (result.error().isPresent()) {
                DataplanetsMod.LOGGER.error("Warning: Failed to encode " + path + " for " + resourceLocation + ": " + result.error().get().message());
                DataplanetsMod.LOGGER.error("Skipping datapack file generation for this object.");
                return; // Skip writing this file
            }

            JsonElement json = result.result().orElseThrow();

            // Write to file with proper formatting
            String jsonString = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(json);

            Files.writeString(objectFile, jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + path + " file: " + resourceLocation, e);
        }

    }

    /**
     * Add a biome to a tag by writing/updating the tag file in the datapack.
     * Tags in datapacks are JSON files that list resource locations.
     */
    private static void addBiomeToTag(
            MinecraftServer server,
            TagKey<Biome> tag,
            ResourceLocation biomeLocation
    ) {
        bindBiomeToTagRuntime(server, tag, biomeLocation);

        Path basePath = ((MinecraftServerAccessor) server).getStorageSource().getLevelPath(LevelResource.DATAPACK_DIR);
        Path dataplanetsFolder = basePath.resolve("dataplanets-generated");
        Path tagFolder = dataplanetsFolder.resolve("data")
                .resolve(tag.location().getNamespace())
                .resolve("tags")
                .resolve("worldgen")
                .resolve("biome");
        Path tagFile = tagFolder.resolve(tag.location().getPath() + ".json");

        try {
            // Create all directories including parent directories of the tag file
            // This handles nested tag paths like "has_structure/mineshaft"
            Files.createDirectories(tagFile.getParent());

            // Read existing tag file if it exists
            List<String> values = new ArrayList<>();
            if (Files.exists(tagFile)) {
                String content = Files.readString(tagFile);
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                if (json.has("values")) {
                    json.getAsJsonArray("values").forEach(element -> values.add(element.getAsString()));
                }
            }

            // Add new biome if not already present
            String biomeId = biomeLocation.toString();
            if (!values.contains(biomeId)) {
                values.add(biomeId);
            }

            // Create tag JSON
            com.google.gson.JsonObject tagJson = new com.google.gson.JsonObject();
            tagJson.addProperty("replace", false);
            com.google.gson.JsonArray valuesArray = new com.google.gson.JsonArray();
            values.forEach(valuesArray::add);
            tagJson.add("values", valuesArray);

            // Write to file
            String jsonString = new com.google.gson.GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(tagJson);

            Files.writeString(tagFile, jsonString);
        } catch (IOException e) {
            throw new RuntimeException("Failed to add biome to tag: " + tag.location(), e);
        }
    }

    /**
     * Bind a biome holder to a tag at runtime by modifying the registry's tag map.
     * This makes the tag change take effect immediately without requiring a datapack reload.
     * Uses the public bindTags method from MappedRegistry.
     */
    @SuppressWarnings("deprecation")
    private static void bindBiomeToTagRuntime(
            MinecraftServer server,
            TagKey<Biome> tag,
            ResourceLocation biomeLocation
    ) {
        try {
            Registry<Biome> biomeRegistry = server.registryAccess().registryOrThrow(Registries.BIOME);
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeLocation);

            // Get the holder for the biome
            Holder.Reference<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(biomeKey);

            // Get existing tag holders
            List<Holder<Biome>> holders = new ArrayList<>();
            Optional<HolderSet.Named<Biome>> existingTag = biomeRegistry.getTag(tag);
            existingTag.ifPresent(namedSet -> namedSet.forEach(holders::add));

            // Add new biome if not already present
            if (!holders.contains(biomeHolder)) {
                holders.add(biomeHolder);
            }

            // If the registry is a MappedRegistry, we can modify its tags
            if (biomeRegistry instanceof MappedRegistry<Biome> mapped) {
                mapped.unfreeze();

                // Create a map with all existing tags plus our updated tag
                Map<TagKey<Biome>, List<Holder<Biome>>> tagMap = new HashMap<>();

                // Copy all existing tags
                mapped.getTags().forEach(pair -> {
                    TagKey<Biome> existingTagKey = pair.getFirst();
                    List<Holder<Biome>> existingHolders = new ArrayList<>();
                    pair.getSecond().forEach(existingHolders::add);
                    tagMap.put(existingTagKey, existingHolders);
                });

                // Add/update our tag
                tagMap.put(tag, holders);

                // Bind all tags using the public bindTags method
                mapped.bindTags(tagMap);

                mapped.freeze();
            }
        } catch (Exception e) {
            // If runtime binding fails, the datapack file will still work after reload
            DataplanetsMod.LOGGER.error("Warning: Could not bind biome to tag at runtime: " + e.getMessage());
            DataplanetsMod.LOGGER.error("Tag will take effect after datapack reload.");
        }
    }

}
