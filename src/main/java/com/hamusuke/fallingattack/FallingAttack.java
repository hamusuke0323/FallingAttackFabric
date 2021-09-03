package com.hamusuke.fallingattack;

import com.hamusuke.fallingattack.enchantment.FallingAttackEnchantment;
import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import com.hamusuke.fallingattack.network.NetworkManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

//TODO sneak when on ground
//TODO rotation first person
//TODO effect
public class FallingAttack implements ModInitializer {
    public static final String MOD_ID = "fallingattack";
    public static final Enchantment FALLING_ATTACK = Registry.register(Registry.ENCHANTMENT, new Identifier(MOD_ID, "falling_attack"), new FallingAttackEnchantment());

    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(NetworkManager.FALLING_ATTACK_C2S_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            PlayerEntityInvoker invoker = (PlayerEntityInvoker) player;
            if (buf.readBoolean()) {
                if (invoker.checkFallingAttack()) {
                    invoker.startFallingAttack();
                    invoker.sendFallingAttackPacket(true);
                }
            } else {
                invoker.stopFallingAttack();
                invoker.sendFallingAttackPacket(false);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(NetworkManager.SYNCHRONIZE_FALLING_ATTACK_C2S_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            ((PlayerEntityInvoker) player).sendSynchronizeFallingAttackPacket();
        });
    }
}
