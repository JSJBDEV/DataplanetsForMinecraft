package ace.actually.dataplanets.mixin;

import ace.actually.dataplanets.space.DynamicSystems;
import ace.actually.dataplanets.interfaces.IUnfreezableRegistry;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin implements IUnfreezableRegistry {

    @Shadow private boolean frozen;

    @Shadow @Final private ResourceKey<? extends Registry<Object>> key;



    @Shadow public abstract boolean containsKey(ResourceKey<Object> p_175392_);

    @Shadow public abstract boolean containsKey(ResourceLocation p_122761_);

    @Shadow private int nextId;

    @Override
    public boolean isRegFrozen() {
        return frozen;
    }

    @Override
    public void setRegFrozen(boolean v) {
        frozen=v;
    }

    @Inject(method = "freeze", at = @At("HEAD"))
    public void preFreeze(CallbackInfoReturnable<Registry<Object>> cir)
    {
        boolean shouldMake = false;
        if(key.location().getPath().equals("dimension_type") && key.location().getNamespace().equals("minecraft"))
        {

            DynamicSystems.DIMENSION_TYPE = (Registry<DimensionType>) this;
            shouldMake=true;
        }
        if(key.location().getPath().equals("worldgen/biome") && key.location().getNamespace().equals("minecraft"))
        {

            DynamicSystems.BIOMES = (Registry<Biome>) this;
            shouldMake=true;
        }
        if(key.location().getPath().equals("worldgen/configured_carver") && key.location().getNamespace().equals("minecraft"))
        {

            DynamicSystems.CONFIGURED_CARVERS = (Registry<ConfiguredWorldCarver<?>>) this;
            shouldMake=true;
        }
        if(key.location().getPath().equals("worldgen/placed_feature") && key.location().getNamespace().equals("minecraft"))
        {

            DynamicSystems.PLACED_FEATURES = (Registry<PlacedFeature>) this;
            shouldMake=true;
        }
        if(key.location().getPath().equals("worldgen/configured_feature") && key.location().getNamespace().equals("minecraft"))
        {

            DynamicSystems.CONFIGURED_FEATURES = (Registry<ConfiguredFeature<?, ?>>) this;
            shouldMake=true;
        }
        if(key.location().getPath().equals("worldgen/noise") && key.location().getNamespace().equals("minecraft"))
        {

            DynamicSystems.NOISE = (Registry<NormalNoise.NoiseParameters>) this;
            shouldMake=true;
        }
        if(key.location().getPath().equals("dimension") && key.location().getNamespace().equals("minecraft"))
        {
            DynamicSystems.LEVEL_STEMS = (Registry<LevelStem>) this;
            shouldMake=true;
        }
        if(shouldMake)
        {
            DynamicSystems.frozeTimes++;
            //System.out.println("frozen "+DynamicSystems.frozeTimes);
            DynamicSystems.loadDynamicResources();
        }

    }
}
