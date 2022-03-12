package com.hamusuke.fallingattack.mixin.client;

import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin<T extends LivingEntity> extends BipedEntityModel<T> {
    PlayerEntityModelMixin(ModelPart root) {
        super(root);
    }

    @Inject(method = "setAngles*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/BipedEntityModel;setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", shift = At.Shift.AFTER), cancellable = true)
    void setAngles(T livingEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        if (livingEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
            PlayerEntityInvoker invoker = (PlayerEntityInvoker) abstractClientPlayerEntity;
            if (invoker.isUsingFallingAttack()) {
                if (invoker.getFallingAttackProgress() < PlayerEntityInvoker.FIRST_FALLING_ATTACK_PROGRESS_TICKS) {
                    if (Float.isNaN(invoker.getYawF())) {
                        invoker.setYawF(abstractClientPlayerEntity.bodyYaw);
                    }

                    abstractClientPlayerEntity.bodyYaw = invoker.getYawF() + 36.0F * invoker.getFallingAttackProgress() * (livingEntity.getMainArm() == Arm.LEFT ? 1 : -1);
                    abstractClientPlayerEntity.headYaw = abstractClientPlayerEntity.bodyYaw;
                } else {
                    this.getArm(livingEntity.getMainArm()).pitch = (float) (-85.0F * Math.PI / 180.0F);
                    this.getArm(livingEntity.getMainArm().getOpposite()).pitch = (float) (80.0F * Math.PI / 180.0F);
                }
            } else if (!Float.isNaN(invoker.getYawF())) {
                invoker.setYawF(Float.NaN);
            }
        }
    }
}
