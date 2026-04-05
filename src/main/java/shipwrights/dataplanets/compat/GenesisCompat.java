package shipwrights.dataplanets.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import shipwrights.dataplanets.systemCreation.CelestialData;

import java.util.List;

public class GenesisCompat implements Compat{
    @Override
    public void addPlanetsToSpace(MinecraftServer server, List<CelestialData> planets) {

    }

    @Override
    public ResourceLocation getSpaceDimensionEffects() {
        return ResourceLocation.parse("genesis:great_unknown");
    }
}
