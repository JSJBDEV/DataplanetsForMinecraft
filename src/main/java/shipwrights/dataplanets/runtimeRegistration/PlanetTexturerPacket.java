package shipwrights.dataplanets.runtimeRegistration;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import shipwrights.dataplanets.texturing.PlanetTextureGenerator;

import java.io.IOException;
import java.util.function.Supplier;


public record PlanetTexturerPacket(CompoundTag tag)
{
	public static PlanetTexturerPacket read(FriendlyByteBuf buffer)
	{
		return new PlanetTexturerPacket(buffer.readAnySizeNbt());
	}
	
	public void write(FriendlyByteBuf buffer)
	{
		buffer.writeNbt(this.tag());
	}
	
	public void handle(Supplier<NetworkEvent.Context> contextGetter)
	{
		NetworkEvent.Context context = contextGetter.get();
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			context.enqueueWork(() -> ClientHandler.handle(this));
		}
		context.setPacketHandled(true);
	}
	
	private static class ClientHandler // making client calls in the static class prevents classloading errors
	{
		private static void handle(PlanetTexturerPacket packet)
		{
			String name = packet.tag().getString("name");
			BlockState state = NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),packet.tag().getCompound("state"));

            try {
                PlanetTextureGenerator.genPlanetTexture(name,state);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
	}
}
