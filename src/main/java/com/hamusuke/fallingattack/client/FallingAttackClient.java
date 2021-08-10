package com.hamusuke.fallingattack.client;

import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import com.hamusuke.fallingattack.network.NetworkManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

@Environment(EnvType.CLIENT)
public class FallingAttackClient implements ClientModInitializer {
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(NetworkManager.START_FALLING_ATTACK_PACKET_ID, (client, handler, buf, responseSender) -> {
            Entity entity = client.world.getEntityById(buf.readVarInt());

            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
                PlayerEntityInvoker invoker = (PlayerEntityInvoker) abstractClientPlayerEntity;

                if (!invoker.isUsingFallingAttack()) {
                    invoker.startFallingAttack();
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(NetworkManager.STOP_FALLING_ATTACK_PACKET_ID, (client, handler, buf, responseSender) -> {
            Entity entity = client.world.getEntityById(buf.readVarInt());

            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity) {
                PlayerEntityInvoker invoker = (PlayerEntityInvoker) abstractClientPlayerEntity;

                if (invoker.isUsingFallingAttack()) {
                    invoker.stopFallingAttack();
                }
            }
        });
    }
}
