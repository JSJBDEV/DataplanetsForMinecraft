package shipwrights.dataplanets.systemCreation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.RandomSource;



/**
 * The core properties of a planet, which cannot be determined through the knowledge of other planet properties
 * <br> <br>
 * All properties use Earth = 1.0 as a reference point
 **/
public record CelestialSource(
        String name,
        double size,
        double orbitDistance,
        double atmosphericDensity,
        double weirdness,
        double seaLevel,
        double terrainRoughness,
        double flavour
) {

    public static final Codec<CelestialSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(CelestialSource::name),
            Codec.DOUBLE.fieldOf("size").forGetter(CelestialSource::size),
            Codec.DOUBLE.fieldOf("orbitDistance").forGetter(CelestialSource::orbitDistance),
            Codec.DOUBLE.fieldOf("atmosphericDensity").forGetter(CelestialSource::atmosphericDensity),
            Codec.DOUBLE.fieldOf("weirdness").forGetter(CelestialSource::weirdness),
            Codec.DOUBLE.fieldOf("seaLevel").forGetter(CelestialSource::seaLevel),
            Codec.DOUBLE.fieldOf("terrainRoughness").forGetter(CelestialSource::terrainRoughness),
            Codec.DOUBLE.fieldOf("flavour").forGetter(CelestialSource::flavour)
    ).apply(instance, CelestialSource::new));

    public static CelestialSource createRandomPlanet(String name, RandomSource random) {
        return builder(name)
                .size(doubleBetween(random, 0.5, 2.0))
                .orbitingDistance(doubleBetween(random, 0.25, 2.0))
                .atmosphericDensity(doubleBetween(random, 0.0, 2.0))
                .weirdness(doubleBetween(random, 0.0, 2.0))
                .seaLevel(doubleBetween(random, 0.0, 2.0))
                .terrainRoughness(doubleBetween(random, 0.0, 2.0))
                .flavour(doubleBetween(random, 0.0, 2.0))
                .build();
    }

    public static CelestialSource createRandomMoon(String name, RandomSource random) {
        return builder(name)
                .size(doubleBetween(random, 0.01, 0.7))
                .orbitingDistance(doubleBetween(random, 0.01, 0.5))
                .atmosphericDensity(doubleBetween(random, 0.0, 1.0))
                .weirdness(doubleBetween(random, 0.0, 3.0))
                .seaLevel(doubleBetween(random, 0.0, 2.0))
                .terrainRoughness(doubleBetween(random, 0.0, 3.0))
                .flavour(doubleBetween(random, 0.0, 3.0))
                .build();
    }

    private static double doubleBetween(RandomSource random, double lower, double upper) {
        return random.nextInt((int)(lower * 100), (int)(upper * 100)) / 100.0;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private double size = 1.0;
        private double distanceFromStar = 1.0;
        private double atmosphericDensity = 1.0;
        private double weirdness = 1.0;
        private double seaLevel = 1.0;
        private double terrainRoughness = 1.0;
        private double flavour = 1.0;

        private Builder(String name) {
            this.name = name;
        }

        public Builder size(double size) {
            this.size = size;
            return this;
        }

        public Builder orbitingDistance(double distanceFromStar) {
            this.distanceFromStar = distanceFromStar;
            return this;
        }

        public Builder atmosphericDensity(double atmosphericDensity) {
            this.atmosphericDensity = atmosphericDensity;
            return this;
        }

        public Builder weirdness(double weirdness) {
            this.weirdness = weirdness;
            return this;
        }

        public Builder seaLevel(double seaLevel) {
            this.seaLevel = seaLevel;
            return this;
        }

        public Builder terrainRoughness(double terrainRoughness) {
            this.terrainRoughness = terrainRoughness;
            return this;
        }

        public Builder flavour(double flavour) {
            this.flavour = flavour;
            return this;
        }

        public CelestialSource build() {
            return new CelestialSource(name, size, distanceFromStar, atmosphericDensity, weirdness, seaLevel, terrainRoughness, flavour);
        }
    }
}
