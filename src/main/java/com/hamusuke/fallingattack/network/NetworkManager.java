package com.hamusuke.fallingattack.network;

import com.hamusuke.fallingattack.FallingAttack;
import net.minecraft.util.Identifier;

public class NetworkManager {
    //public static final Identifier REQUEST_FALLING_ATTACK_PACKET_ID = new Identifier(FallingAttack.MOD_ID, "falling_attack_packet");
    public static final Identifier START_FALLING_ATTACK_PACKET_ID = new Identifier(FallingAttack.MOD_ID, "start_falling_attack_packet");
    public static final Identifier STOP_FALLING_ATTACK_PACKET_ID = new Identifier(FallingAttack.MOD_ID, "stop_falling_attack_packet");
}
