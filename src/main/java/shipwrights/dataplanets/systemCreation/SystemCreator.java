package shipwrights.dataplanets.systemCreation;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import shipwrights.dataplanets.PlanetLookup;
import shipwrights.dataplanets.compat.Compat;
import shipwrights.dataplanets.mixin.SpaceRegistryInvoker;
import shipwrights.dataplanets.systemCreation.naming.SystemNameGenerator;
import shipwrights.dataplanets.systemCreation.dimension.biome.BiomeCreator;
import shipwrights.dataplanets.systemCreation.dimension.DimensionTypeCreator;
import shipwrights.dataplanets.systemCreation.dimension.noise.TerrainGenCreator;
import shipwrights.dataplanets.runtimeRegistration.RegistryUtil;
import shipwrights.dataplanets.runtimeRegistration.ServerPhase;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.transformProvider.CelestialTransformProvider;
import shipwrights.genesis.space.transformProvider.OrbitingTransformProvider;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;
import shipwrights.genesis.space.type.CelestialType;

import java.util.ArrayList;
import java.util.List;

import static shipwrights.dataplanets.DataplanetsMod.MOD_ID;

public class SystemCreator {

    public void createSystem(MinecraftServer server, boolean scientificNamingStyle, ServerPhase phase) {
        SystemCreationContext context = new SystemCreationContext(server, scientificNamingStyle, phase);

        List<PlanetSource> sources = createPlanetSources(context);

        List<PlanetData> planets = new ArrayList<>();

        for (var source : sources) {
            planets.add(createPlanet(source, context));
            PlanetLookup.store(server, source);
        }

        context.compat.addPlanetsToSpace(server, planets);
    }

    private static List<PlanetSource> createPlanetSources(SystemCreator.SystemCreationContext context) {
        int planetCount = context.random.nextInt(4, 9);
        List<PlanetSource> output = new ArrayList<>();
        for (int i = 0; i < planetCount; i++) {
            output.add(PlanetSource.createRandom(context.nextPlanetName(), context.random));
        }

        return output;
    }

    public PlanetData createPlanet(PlanetSource source, SystemCreationContext context) {
        PlanetData planetData = PlanetData.fromPlanetSource(source);

        Holder<DimensionType> dimensionTypeHolder = DimensionTypeCreator.createAndRegisterDimensionType(context, planetData);

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomeList = new BiomeCreator().createAndRegisterBiomes(context, planetData);

        Holder<NoiseGeneratorSettings> noiseSettings = TerrainGenCreator.createFromPlanetData(planetData, context);

        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(biomeList));
        NoiseBasedChunkGenerator noiseBasedChunkGenerator = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);

        LevelStem stem = new LevelStem(dimensionTypeHolder, noiseBasedChunkGenerator);

        RegistryUtil.registerLevelStem(context.server, ResourceLocation.fromNamespaceAndPath(MOD_ID, planetData.name()), stem, context.serverPhase);

        Celestial celestial = new Celestial(
                new OrbitingTransformProvider(
                        ResourceLocation.tryParse("genesis:sun"),
                        4443,
                        planetData.distanceFromStar() * 15_000,
                        planetData.orbitalPeriod() * 4_608_000,
                        24000
                ),
                ResourceLocation.fromNamespaceAndPath("dataplanets",planetData.name()),
                BuiltinCelestialTypes.BODY,
                planetData.size() * 96,
                planetData.gravity(),
                planetData.color().red(),
                planetData.color().green(),
                planetData.color().blue()
        );

        SpaceRegistryInvoker spaceRegistryInvoker = (SpaceRegistryInvoker) GenesisMod.SPACE_REGISTRY;
        spaceRegistryInvoker.addBody(ResourceLocation.fromNamespaceAndPath("dataplanets",planetData.name()),celestial);
        return planetData;
    }


    public static class SystemCreationContext {
        public final MinecraftServer server;
        public final RandomSource random = RandomSource.create();
        public final String systemName;
        public int currentPlanetIndex = 0;
        public Compat compat = Compat.get();
        public final ServerPhase serverPhase;

        public SystemCreationContext(MinecraftServer server, boolean scientificNameStyle, ServerPhase phase) {
            this.server = server;
            this.systemName = SystemNameGenerator.get(scientificNameStyle).generate(random);
            this.serverPhase = phase;
        }

        public String nextPlanetName() {
            return systemName + SystemNameGenerator.ALL_LETTERS.charAt(currentPlanetIndex++);
        }
    }
}
