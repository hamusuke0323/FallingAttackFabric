package com.hamusuke.fallingattack.mixin;

import com.google.common.collect.Lists;
import com.hamusuke.fallingattack.invoker.ServerWorldInvoker;
import com.hamusuke.fallingattack.math.FallingAttackShockWave;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements ServerWorldInvoker {
    private final List<FallingAttackShockWave> shockWaves = Lists.newArrayList();

    ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        synchronized (this.shockWaves) {
            this.shockWaves.forEach(FallingAttackShockWave::tick);
            this.shockWaves.removeIf(FallingAttackShockWave::isDead);
        }
    }

    @Override
    public void summonShockWave(FallingAttackShockWave shockWave) {
        this.shockWaves.add(shockWave);
    }
}
