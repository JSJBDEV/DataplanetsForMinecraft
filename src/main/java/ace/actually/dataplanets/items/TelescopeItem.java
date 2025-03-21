package ace.actually.dataplanets.items;

import ace.actually.dataplanets.Dataplanets;
import ace.actually.dataplanets.StarSystemCreator;
import ace.actually.dataplanets.interfaces.IUnfreezableRegistry;
import argent_matter.gcyr.common.data.GCYRBiomes;
import argent_matter.gcyr.common.data.GCYRBlocks;
import argent_matter.gcyr.common.data.GCYRDimensionTypes;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Lifecycle;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.data.worldgen.SurfaceRuleData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.BiomeGenerationSettingsBuilder;
import net.minecraftforge.event.level.LevelEvent;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TelescopeItem extends Item {
    public TelescopeItem(Properties pProperties) {
        super(pProperties);
    }

    private static ResourceKey<Biome> planetDataToBiome(MinecraftServer server, CompoundTag planetData)
    {
        RegistryAccess.Frozen registryAccess = server.registryAccess();

        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);

        Registry<ConfiguredWorldCarver<?>> carversRegistry = server.registryAccess().registryOrThrow(Registries.CONFIGURED_CARVER);
        Holder.Reference<ConfiguredWorldCarver<?>> canyon = carversRegistry.getHolderOrThrow(Carvers.CANYON);
        Holder.Reference<ConfiguredWorldCarver<?>> cave = carversRegistry.getHolderOrThrow(Carvers.CAVE);
        Holder.Reference<ConfiguredWorldCarver<?>> cave_extra = carversRegistry.getHolderOrThrow(Carvers.CAVE_EXTRA_UNDERGROUND);

        Registry<PlacedFeature> placedFeatureRegistry = server.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        Holder.Reference<PlacedFeature> dripstone = placedFeatureRegistry.getHolder(ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.tryParse("large_dripstone"))).get();
        Holder.Reference<PlacedFeature> dripstone_cluster = placedFeatureRegistry.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.tryParse("dripstone_cluster"))).get();
        Holder.Reference<PlacedFeature> pointed_dripstone = placedFeatureRegistry.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,ResourceLocation.tryParse("pointed_dripstone"))).get();

        Biome biome = new Biome.BiomeBuilder()
                .downfall(0)
                .temperature(0.93f)
                .hasPrecipitation(false)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(planetData.getInt("skyColour"))
                        .fogColor(planetData.getInt("fogColour"))
                        .waterColor(planetData.getInt("waterColour"))
                        .waterFogColor(planetData.getInt("waterFogColour"))
                        .grassColorOverride(planetData.getInt("grassColour"))
                        .foliageColorOverride(planetData.getInt("foliage")).build())
                .mobSpawnSettings(new MobSpawnSettings.Builder().build())
                .generationSettings(new BiomeGenerationSettingsBuilder(BiomeGenerationSettings.EMPTY)
                        .addCarver(GenerationStep.Carving.AIR,canyon)
                        .addCarver(GenerationStep.Carving.AIR,cave)
                        .addCarver(GenerationStep.Carving.AIR,cave_extra)
                        .addFeature(0,dripstone)
                        .addFeature(0,dripstone_cluster)
                        .addFeature(0,pointed_dripstone).build()).build();



        ResourceKey<Biome> biomeKey = ResourceKey.create(biomeRegistry.key(), ResourceLocation.tryBuild("dataplanets",planetData.getString("name")+"_terrain"));


        ((IUnfreezableRegistry) biomeRegistry).setRegFrozen(false);
        ((MappedRegistry<Biome>) biomeRegistry).register(
                biomeKey,
                biome,
                Lifecycle.experimental() // use built-in registration info for now
        );
        ((IUnfreezableRegistry) biomeRegistry).setRegFrozen(true);

        return biomeKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if(!pLevel.isClientSide)
        {
            if(pPlayer.isCrouching())
            {
                makeDynamicWorld(pLevel.getServer(), pLevel.getServer().getCommandStorage().get(StarSystemCreator.SYSTEM_DATA).getCompound("sz633").getCompound("sz633d"));
            }
            else
            {
                //StarSystemCreator.makeDynamicBiome(pLevel.getServer());
            }

        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }



    private void makeDynamicWorld(MinecraftServer server,CompoundTag planetData)
    {
        RegistryAccess.Frozen registryAccess = server.registryAccess();
        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        Registry<NormalNoise.NoiseParameters> noise = registryAccess.registryOrThrow(Registries.NOISE);

        Holder.Reference<Biome> biomeHolder = biomeRegistry.getHolderOrThrow(planetDataToBiome(server,planetData));

        NoiseGeneratorSettings settings = new NoiseGeneratorSettings(
                NoiseSettings.create(-64, 384, 2, 2),
                GCYRBlocks.MOON_COBBLESTONE.getDefaultState(),
                Blocks.AIR.defaultBlockState(),
                new NoiseRouter(
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.add(
                                DensityFunctions.yClampedGradient(-64,320,1,-1),
                                DensityFunctions.noise(noise.getHolderOrThrow(Noises.GRAVEL),planetData.getFloat("nr1"),planetData.getFloat("nr2"))),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0),
                        DensityFunctions.constant(0)),
                SurfaceRuleData.overworld(),
                new OverworldBiomeBuilder().spawnTarget(),
                63,
                false,
                true,
                true,
                false
        );
        NoiseBasedChunkGenerator noiseBasedChunkGenerator = new NoiseBasedChunkGenerator(new FixedBiomeSource(biomeHolder), Holder.direct(settings));



        //DimensionType type = new DimensionType(OptionalLong.empty(),true,false,false,true,1.0,true,false,-64,384,384, BlockTags.INFINIBURN_OVERWORLD,ResourceLocation.fromNamespaceAndPath("gcyr","luna"),0,new DimensionType.MonsterSettings(true,false, UniformInt.of(0,7),0));
        //ResourceKey<DimensionType> dimensionTypeId = ResourceKey.create(Registries.DIMENSION_TYPE,ResourceLocation.fromNamespaceAndPath("dataplanets","coolworld"));
        ChunkProgressListener listener = server.progressListenerFactory.create(server.getWorldData().getGameRules().getInt(GameRules.RULE_SPAWN_RADIUS));
        Registry<DimensionType> dimensionTypes = server.registryAccess().registryOrThrow(Registries.DIMENSION_TYPE);
        Holder.Reference<DimensionType> holder = dimensionTypes.getHolderOrThrow(GCYRDimensionTypes.SPACE_TYPE);
        ResourceKey<LevelStem> resourcekey = ResourceKey.create(Registries.LEVEL_STEM, ResourceLocation.tryBuild("dataplanets","coolworld"));
        ServerLevelData serverleveldata = server.getWorldData().overworldData();

        LevelStem stem = new LevelStem(holder,noiseBasedChunkGenerator);

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, resourcekey.location());
        DerivedLevelData derivedleveldata = new DerivedLevelData(server.getWorldData(), serverleveldata);
        ServerLevel serverlevel1 = new ServerLevel(server, Util.backgroundExecutor(), server.storageSource, derivedleveldata, dimensionKey, stem, listener, server.getWorldData().isDebugWorld(), BiomeManager.obfuscateSeed(server.getWorldData().worldGenOptions().seed()), ImmutableList.of(), false, server.overworld().getRandomSequences());
        server.overworld().getWorldBorder().addListener(new BorderChangeListener.DelegateBorderChangeListener(serverlevel1.getWorldBorder()));

        server.levels.put(dimensionKey, serverlevel1);
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(server.levels.get(resourcekey)));
        //https://github.com/iPortalTeam/DimLib/blob/1.21/src/main/java/qouteall/dimlib/DynamicDimensionsImpl.java

        Registry<LevelStem> levelStemRegistry = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        ((IUnfreezableRegistry) levelStemRegistry).setRegFrozen(false);
        ((MappedRegistry<LevelStem>) levelStemRegistry).register(
                resourcekey,
                stem,
                Lifecycle.experimental() // use built-in registration info for now
        );
        ((IUnfreezableRegistry) levelStemRegistry).setRegFrozen(true);

    }

    @Override
    public void appendHoverText(ItemStack p_41421_, @Nullable Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_) {
        super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);
        p_41423_.add(Component.empty().append("WIP: on-the-fly dimension generation"));
    }
}
