package shipwrights.dataplanets.registry;

import shipwrights.dataplanets.DataplanetsMod;
import shipwrights.dataplanets.blocks.*;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class DPBlocks {

    public static BlockEntry<GasBlock> DENSE_GAS = DataplanetsMod.REGISTRATE.block("dense_gas", a->new GasBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noLootTable().air().noOcclusion())).register();

    /// do not delete
    public static void init() {}
}
