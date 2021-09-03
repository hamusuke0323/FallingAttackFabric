package com.hamusuke.fallingattack.mixin;

import com.hamusuke.fallingattack.network.NetworkManager;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntityMixin {
    @Shadow
    @Final
    public MinecraftServer server;

    @Shadow
    public ServerPlayNetworkHandler networkHandler;

    ServerPlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("UsingFallingAttack", this.fallingAttack);
        nbt.putFloat("StartFallingAttackYPos", this.yPosWhenStartFallingAttack);
        nbt.putInt("FallingAttackProgress", this.fallingAttackProgress);
        nbt.putFloat("StartFallingAttackYaw", this.storeYaw);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        this.fallingAttack = nbt.getBoolean("UsingFallingAttack");
        this.yPosWhenStartFallingAttack = nbt.getFloat("StartFallingAttackYPos");
        this.fallingAttackProgress = nbt.getInt("FallingAttackProgress");
        this.storeYaw = nbt.getFloat("StartFallingAttackYaw");
    }

    public void sendFallingAttackPacket(boolean start) {
        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeVarInt(this.getId());
        packetByteBuf.writeBoolean(start);
        this.server.getPlayerManager().getPlayerList().forEach(serverPlayerEntity -> {
            serverPlayerEntity.networkHandler.sendPacket(new CustomPayloadS2CPacket(NetworkManager.FALLING_ATTACK_S2C_PACKET_ID, packetByteBuf));
        });
    }

    public void sendSynchronizeFallingAttackPacket() {
        PacketByteBuf packetByteBuf = PacketByteBufs.create();
        packetByteBuf.writeBoolean(this.fallingAttack);
        packetByteBuf.writeFloat(this.yPosWhenStartFallingAttack);
        packetByteBuf.writeInt(this.fallingAttackProgress);
        packetByteBuf.writeFloat(this.storeYaw);
        this.networkHandler.sendPacket(new CustomPayloadS2CPacket(NetworkManager.SYNCHRONIZE_FALLING_ATTACK_S2C_PACKET_ID, packetByteBuf));
    }
}
