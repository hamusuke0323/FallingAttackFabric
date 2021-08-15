package com.hamusuke.fallingattack.mixin;

import com.hamusuke.fallingattack.network.NetworkManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntityMixin {
    @Shadow
    @Final
    public MinecraftServer server;

    ServerPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    public void sendFallingAttackPacket(boolean start) {
        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeVarInt(this.getId());
        packetByteBuf.writeBoolean(start);
        this.server.getPlayerManager().getPlayerList().forEach(serverPlayerEntity -> {
            serverPlayerEntity.networkHandler.sendPacket(new CustomPayloadS2CPacket(NetworkManager.FALLING_ATTACK_S2C_PACKET_ID, packetByteBuf));
        });
    }
}
