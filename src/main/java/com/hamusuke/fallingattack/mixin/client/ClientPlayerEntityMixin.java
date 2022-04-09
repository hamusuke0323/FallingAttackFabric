package com.hamusuke.fallingattack.mixin.client;

import com.hamusuke.fallingattack.mixin.PlayerEntityMixin;
import com.hamusuke.fallingattack.network.NetworkManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Environment(EnvType.CLIENT)
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin extends PlayerEntityMixin {
    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;

    ClientPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    public void sendFallingAttackPacket(boolean start) {
        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeBoolean(start);
        this.networkHandler.sendPacket(new CustomPayloadC2SPacket(NetworkManager.FALLING_ATTACK_C2S_PACKET_ID, packetByteBuf));
    }

    public void sendSynchronizeFallingAttackPacket() {
        this.networkHandler.sendPacket(new CustomPayloadC2SPacket(NetworkManager.SYNCHRONIZE_FALLING_ATTACK_C2S_PACKET_ID, PacketByteBufs.empty()));
    }
}
