package com.hamusuke.fallingattack.mixin.client;

import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(HeldItemFeatureRenderer.class)
public abstract class HeldItemFeatureRendererMixin<T extends LivingEntity, M extends EntityModel<T> & ModelWithArms> extends FeatureRenderer<T, M> {
    HeldItemFeatureRendererMixin(FeatureRendererContext<T, M> context) {
        super(context);
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    void renderItem(LivingEntity entity, ItemStack stack, ModelTransformation.Mode transformationMode, Arm arm, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!stack.isEmpty() && entity instanceof AbstractClientPlayerEntity && ((PlayerEntityInvoker) entity).getFallingAttackProgress() >= PlayerEntityInvoker.FIRST_FALLING_ATTACK_PROGRESS_TICKS) {
            matrices.push();
            boolean bl = arm == Arm.LEFT;
            this.getContextModel().setArmAngle(arm, matrices);
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(90.0F));
            matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(180.0F));
            matrices.translate((float) (bl ? -1 : 1) / 16.0F, 0.125D, 0.4D);
            MinecraftClient.getInstance().getHeldItemRenderer().renderItem(entity, stack, transformationMode, bl, matrices, vertexConsumers, light);
            matrices.pop();
            ci.cancel();
        }
    }
}
