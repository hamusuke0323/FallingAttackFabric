package com.hamusuke.fallingattack.mixin.client;

import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import com.mojang.authlib.GameProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(OtherClientPlayerEntity.class)
public abstract class OtherClientPlayerEntityMixin extends AbstractClientPlayerEntity implements PlayerEntityInvoker {
    OtherClientPlayerEntityMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    void tickMovementV(CallbackInfo ci) {
        if (this.isUsingFallingAttack()) {
            if (this.getFallingAttackProgress() < FIRST_FALLING_ATTACK_PROGRESS_TICKS) {
                this.setFallingAttackProgress(this.getFallingAttackProgress() + 1);
            } else if (this.getFallingAttackProgress() == FIRST_FALLING_ATTACK_PROGRESS_TICKS) {
                if (this.isTouchingWater() || this.isInLava() || this.world.getBottomY() > this.getBlockY()) {
                    this.stopFallingAttack();
                } else if (this.onGround) {
                    this.setFallingAttackProgress(this.getFallingAttackProgress() + 1);
                }
            } else if (this.getFallingAttackProgress() < FALLING_ATTACK_END_TICKS) {
                this.setFallingAttackProgress(this.getFallingAttackProgress() + 1);
            } else if (this.isUsingFallingAttack()) {
                this.stopFallingAttack();
            }
        }
    }
}
