package shipwrights.dataplanets.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import shipwrights.dataplanets.DataplanetsConfig;
import shipwrights.dataplanets.PlanetLookup;
import shipwrights.dataplanets.systemCreation.CelestialSource;
import shipwrights.dataplanets.systemCreation.StarData;
import shipwrights.dataplanets.systemCreation.SystemCreator;
import shipwrights.dataplanets.runtimeRegistration.ServerPhase;
import shipwrights.genesis.GenesisMod;

public class PlanetCreationItem extends Item {
    public PlanetCreationItem(Properties arg) {
        super(arg);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level arg, Player arg2, InteractionHand arg3) {
        if (arg instanceof ServerLevel level && arg3 == InteractionHand.MAIN_HAND)
        {
            SystemCreator creator = new SystemCreator();
            SystemCreator.SystemCreationContext context = new SystemCreator.SystemCreationContext(level.getServer(), true, ServerPhase.running);

            StarData starData = StarData.createRandom(context);
            SystemCreator.registerStaticToGenesis(starData,context);

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
            arg2.sendSystemMessage(Component.literal("A new system was found at "+starData.pos()[0]+" "+starData.pos()[1]+" "+starData.pos()[2]+"!"));
            arg2.getItemInHand(arg3).shrink(1);
        }
        return InteractionResultHolder.success(arg2.getItemInHand(arg3));
    }
}
