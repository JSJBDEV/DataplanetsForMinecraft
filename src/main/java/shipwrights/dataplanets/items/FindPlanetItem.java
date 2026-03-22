package shipwrights.dataplanets.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import shipwrights.dataplanets.PlanetLookup;
import shipwrights.genesis.GenesisMod;
import shipwrights.genesis.space.Celestial;

import java.util.Collection;
import java.util.List;

public class FindPlanetItem extends Item {
    public FindPlanetItem(Properties p_41383_) {
        super(p_41383_);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level p_41432_, Player p_41433_, InteractionHand p_41434_) {
        if(p_41432_ instanceof ServerLevel level && p_41434_==InteractionHand.MAIN_HAND)
        {
            List<Celestial> celestials = GenesisMod.SPACE_REGISTRY.getAll();
            celestials.forEach(a->
            {
                Vector3dc v = a.getPosition(level.getGameTime());
                p_41433_.sendSystemMessage(Component.literal(a.ID().toString()+": at "+v.x()+" "+v.y()+" "+v.z()));
            });
        }
        return super.use(p_41432_, p_41433_, p_41434_);
    }
}
