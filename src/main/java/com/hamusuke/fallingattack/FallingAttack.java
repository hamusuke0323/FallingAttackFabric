package com.hamusuke.fallingattack;

import com.hamusuke.fallingattack.enchantment.FallingAttackEnchantment;
import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import com.hamusuke.fallingattack.network.NetworkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class FallingAttack implements ModInitializer {
    public static final String MOD_ID = "fallingattack";
    public static final Enchantment FALLING_ATTACK = Registry.register(Registry.ENCHANTMENT, new Identifier(MOD_ID, "falling_attack"), new FallingAttackEnchantment());

    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(NetworkManager.START_FALLING_ATTACK_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            PlayerEntityInvoker invoker = (PlayerEntityInvoker) player;
            invoker.startFallingAttack();
            invoker.sendStartFallingAttackPacket();
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkManager.STOP_FALLING_ATTACK_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            PlayerEntityInvoker invoker = (PlayerEntityInvoker) player;
            invoker.stopFallingAttack();
            invoker.sendStopFallingAttackPacket();
        });
    }
}
