package shipwrights.dataplanets.registry;

import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.items.*;
import com.tterrag.registrate.util.entry.ItemEntry;

public class DPItems {

    static {
        DataplanetsMod.REGISTRATE.defaultCreativeTab("dataplanets").register();
    }

    public static ItemEntry<PlanetCreationItem> CREATE_PLANET_ITEM = DataplanetsMod.REGISTRATE.item("test_planet_creator", PlanetCreationItem::new).properties((a)->a).lang("Test Planet Creator").register();
    public static ItemEntry<TelescopeItem> TELESCOPE_ITEM = DataplanetsMod.REGISTRATE.item("portable_telescope",TelescopeItem::new).properties((a)->a).lang("Portable Telescope").register();

    /// do not delete
    public static void init() {}
}
