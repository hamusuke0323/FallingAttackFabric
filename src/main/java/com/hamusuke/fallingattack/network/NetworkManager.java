package com.hamusuke.fallingattack.network;

import com.hamusuke.fallingattack.FallingAttack;
import net.minecraft.util.Identifier;

public class NetworkManager {
    public static final Identifier FALLING_ATTACK_S2C_PACKET_ID = new Identifier(FallingAttack.MOD_ID, "falling_attack_s2c_packet");
    public static final Identifier FALLING_ATTACK_C2S_PACKET_ID = new Identifier(FallingAttack.MOD_ID, "falling_attack_c2s_packet");
}
