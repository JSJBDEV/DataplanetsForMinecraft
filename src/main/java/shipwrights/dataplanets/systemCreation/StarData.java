package shipwrights.dataplanets.systemCreation;

import net.minecraft.util.RandomSource;

public record StarData(int[] gradient, String name, double size, int[] pos) {

    public static StarData createRandom(SystemCreator.SystemCreationContext context)
    {
        RandomSource random = context.random;
        int[] g = new int[]{random.nextInt(255),random.nextInt(255), random.nextInt(255),random.nextInt(255),random.nextInt(255), random.nextInt(255)};
        int[] p = new int[]{random.nextInt(-1000000,1000000),0,random.nextInt(-1000000,1000000)};
        double s = random.nextDouble();

        return new StarData(g,context.systemName,s,p);
    }
}
