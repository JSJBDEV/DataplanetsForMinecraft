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
import shipwrights.dataplanets.compat.Compat;
import shipwrights.dataplanets.systemCreation.naming.SystemNameGenerator;
import shipwrights.dataplanets.systemCreation.dimension.biome.BiomeCreator;
import shipwrights.dataplanets.systemCreation.dimension.DimensionTypeCreator;
import shipwrights.dataplanets.systemCreation.dimension.noise.TerrainGenCreator;
import shipwrights.dataplanets.runtimeRegistration.RegistryUtil;
import shipwrights.dataplanets.runtimeRegistration.ServerPhase;
import shipwrights.genesis.space.Celestial;
import shipwrights.genesis.space.properties.Atmosphere;
import shipwrights.genesis.space.properties.PlanetColorPalette;
import shipwrights.genesis.space.properties.PlanetProperties;
import shipwrights.genesis.space.properties.StarProperties;
import shipwrights.genesis.space.transformProvider.OrbitingTransformProvider;
import shipwrights.genesis.space.transformProvider.StaticTransformProvider;
import shipwrights.genesis.space.type.BuiltinCelestialTypes;

import java.util.ArrayList;
import java.util.List;

import static shipwrights.dataplanets.DataplanetsMod.MOD_ID;

public class SystemCreator {

    private static List<CelestialSource> createPlanetSources(SystemCreator.SystemCreationContext context) {
        int planetCount = context.random.nextInt(4, 9);
        List<CelestialSource> output = new ArrayList<>();
        for (int i = 0; i < planetCount; i++) {
            output.add(CelestialSource.createRandomPlanet(context.nextPlanetName(), context.random));
        }

        return output;
    }

    public CelestialData createBody(CelestialSource source, SystemCreationContext context, String orbiting) {
        CelestialData celestialData = CelestialData.fromPlanetSource(source);

        Holder<DimensionType> dimensionTypeHolder = DimensionTypeCreator.createAndRegisterDimensionType(context, celestialData);

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> biomeList = new BiomeCreator().createAndRegisterBiomes(context, celestialData);

        Holder<NoiseGeneratorSettings> noiseSettings = TerrainGenCreator.createFromPlanetData(celestialData, context);

        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(new Climate.ParameterList<>(biomeList));
        NoiseBasedChunkGenerator noiseBasedChunkGenerator = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);

        LevelStem stem = new LevelStem(dimensionTypeHolder, noiseBasedChunkGenerator);

        RegistryUtil.registerLevelStem(context.server, ResourceLocation.fromNamespaceAndPath(MOD_ID, celestialData.name()), stem, context.serverPhase);

        registerPlanetToGenesis(celestialData,context,orbiting);

        return celestialData;
    }

    public CelestialData createBody(CelestialSource source, SystemCreationContext context)
    {
        return createBody(source,context,"genesis:sun");
    }

    public static void registerStaticToGenesis(StarData starData,SystemCreationContext context)
    {
        ResourceLocation rv = ResourceLocation.fromNamespaceAndPath("dataplanets", starData.name());

        int[] g = starData.gradient();
        int[] p = starData.pos();

        StarProperties starProperties = new StarProperties(g[0],g[1],g[2],g[3],g[4],g[5]);
        Celestial celestial = new Celestial(
               new StaticTransformProvider(p[0],p[1],p[2]),

                BuiltinCelestialTypes.STAR,
                starData.size() * 1000,
                2,
                0.5f,
                0.5f,
                0.5f,
                starProperties
        );

        RegistryUtil.registerGenesisFiles(context.server,celestial,rv,false);
    }

    public static void registerPlanetToGenesis(CelestialData celestialData, SystemCreationContext context,String orbiting)
    {
        Atmosphere atmosphere = new Atmosphere(celestialData.atmosphericDensity(), celestialData.temperature(),false,false,new PlanetColorPalette.RGB(celestialData.color().red(), celestialData.color().green(), celestialData.color().blue()));
        PlanetProperties planetProperties = new PlanetProperties(atmosphere);

        ResourceLocation rv = ResourceLocation.fromNamespaceAndPath("dataplanets", celestialData.name());

        Celestial celestial = new Celestial(
                new OrbitingTransformProvider(
                        ResourceLocation.tryParse(orbiting),
                        celestialData.name().hashCode(),
                        celestialData.distanceFromStar() * 15_000,
                        celestialData.orbitalPeriod() * 4_608_000,
                        24000
                ),

                BuiltinCelestialTypes.BODY,
                celestialData.size() * 96,
                celestialData.gravity(),
                celestialData.color().red(),
                celestialData.color().green(),
                celestialData.color().blue(),
                planetProperties
        );

        RegistryUtil.registerGenesisFiles(context.server,celestial,rv,true);


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

        public String currentPlanetName()
        {
            return systemName + SystemNameGenerator.ALL_LETTERS.charAt(--currentPlanetIndex);
        }
    }
}
