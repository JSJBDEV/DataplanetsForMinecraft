package shipwrights.dataplanets.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class GenesisCompat implements Compat{
    @Override
    public void addPlanetsToSpace(MinecraftServer server, List<shipwrights.dataplanets.systemCreation.PlanetData> planets) {

    }

    @Override
    public ResourceLocation getSpaceDimensionEffects() {
        return ResourceLocation.parse("genesis:great_unknown");
    }
}
