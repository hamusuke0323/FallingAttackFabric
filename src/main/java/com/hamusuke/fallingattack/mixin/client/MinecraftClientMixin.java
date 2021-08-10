package com.hamusuke.fallingattack.mixin.client;

import com.hamusuke.fallingattack.FallingAttack;
import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    void doAttack(CallbackInfo ci) {
        PlayerEntityInvoker invoker = (PlayerEntityInvoker) this.player;

        if (!invoker.isUsingFallingAttack()) {
            ItemStack itemStack = this.player.getMainHandStack();
            if (EnchantmentHelper.getLevel(FallingAttack.FALLING_ATTACK, itemStack) > 0 && ((PlayerEntityInvoker) this.player).checkFallingAttack()) {
                ci.cancel();
            }
        } else if (invoker.isUsingFallingAttack()) {
            invoker.stopFallingAttack();
        }
    }
}
