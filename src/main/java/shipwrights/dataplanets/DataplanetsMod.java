package shipwrights.dataplanets;

import com.tterrag.registrate.Registrate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shipwrights.dataplanets.registry.DPBlocks;
import shipwrights.dataplanets.registry.DPItems;
import shipwrights.dataplanets.runtimeRegistration.PlanetTexturerPacket;
import shipwrights.dataplanets.systemCreation.naming.FantasySystemNameGenerator;
import shipwrights.dataplanets.runtimeRegistration.RegistryUtil;
import shipwrights.genesis.GenesisMod;

@Mod.EventBusSubscriber
@Mod(DataplanetsMod.MOD_ID)
public class DataplanetsMod {
    public static final String MOD_ID = "dataplanets";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final FantasySystemNameGenerator FANTASY_SYSTEM_NAME_GENERATOR = new FantasySystemNameGenerator();

    public static final Registrate REGISTRATE = Registrate.create(MOD_ID);

    public static final ResourceLocation MUTABLE_DATA = ResourceLocation.fromNamespaceAndPath("dataplanets","mutable_data");

    public DataplanetsMod(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, DataplanetsConfig.CONFIG_SPEC);
        DPItems.init();
        DPBlocks.init();
        DPPackets.init();
    }


    @SubscribeEvent
    public static void onDataReload(AddReloadListenerEvent event) {
        event.addListener(FANTASY_SYSTEM_NAME_GENERATOR);
    }

    @SubscribeEvent
    public static void playerJoins(PlayerEvent.PlayerLoggedInEvent event) {

        GenesisMod.getCelestialRegistry(event.getEntity().level()).keySet().forEach(a->
        {
            if(a.getNamespace().equals("dataplanets"))
            {
                RegistryUtil.sendTexturesToClient(a);
            }
        });

    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        RegistryUtil.setupDatapackFolder(server);
    }


}
