package com.hamusuke.fallingattack.mixin;

import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import com.hamusuke.fallingattack.invoker.ServerWorldInvoker;
import com.hamusuke.fallingattack.math.FallingAttackShockWave;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity implements PlayerEntityInvoker {
    @Shadow
    @Final
    private PlayerAbilities abilities;

    @Shadow
    public abstract void resetLastAttackedTicks();

    @Shadow
    public abstract void addExhaustion(float exhaustion);

    @Shadow
    public abstract void stopFallFlying();

    @Shadow
    public abstract void incrementStat(Stat<?> stat);

    protected boolean fallingAttack;
    protected float yPosWhenStartFallingAttack;
    protected int fallingAttackProgress;
    protected int fallingAttackCooldown;
    protected float storeYaw = Float.NaN;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    void tickV(CallbackInfo ci) {
        if (!this.isUsingFallingAttack() && this.fallingAttackCooldown > 0) {
            this.fallingAttackCooldown--;
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    void tickMovementV(CallbackInfo ci) {
        if (this.isUsingFallingAttack()) {
            if (this.fallingAttackProgress < FIRST_FALLING_ATTACK_PROGRESS_TICKS) {
                if (this.fallingAttackProgress == 0) {
                    this.setVelocity(new Vec3d(0.0D, 1.0D, 0.0D));
                } else if (this.fallingAttackProgress > FIRST_FALLING_ATTACK_PROGRESS_TICKS / 2) {
                    this.setVelocity(Vec3d.ZERO);
                }

                if (this.fallingAttackProgress == FIRST_FALLING_ATTACK_PROGRESS_TICKS - 1) {
                    this.yPosWhenStartFallingAttack = (float) this.getY();
                }

                this.fallingAttackProgress++;
            } else if (this.fallingAttackProgress == FIRST_FALLING_ATTACK_PROGRESS_TICKS) {
                if (this.isTouchingWater() || this.isInLava() || this.world.getBottomY() > this.getBlockY()) {
                    this.stopFallingAttack();
                    this.setVelocity(Vec3d.ZERO);
                } else if (this.onGround) {
                    this.fallingAttackProgress++;

                    ItemStack sword = this.getMainHandStack();
                    Item item = sword.getItem();
                    if (!this.world.isClient() && (Object) this instanceof ServerPlayerEntity serverPlayer && item instanceof SwordItem) {
                        sword.damage(1, serverPlayer, e -> e.sendEquipmentBreakStatus(EquipmentSlot.MAINHAND));
                        this.incrementStat(Stats.USED.getOrCreateStat(item));
                        if (sword.isEmpty()) {
                            this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                        }

                        this.world.playSoundFromEntity(null, this, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                        float d = this.computeFallingAttackDistance();
                        Box box = this.getBoundingBox().expand(8.0D * d * 0.1D, 0.0D, 8.0D * d * 0.1D);
                        ((ServerWorldInvoker) this.world).summonShockWave(new FallingAttackShockWave(serverPlayer, sword, new Box(box.minX, box.minY, box.minZ, box.maxX, box.minY + 0.85D, box.maxZ), this::computeFallingAttackDamage, this::computeKnockbackStrength));
                    }

                    this.resetLastAttackedTicks();
                    this.addExhaustion(0.1F);
                } else {
                    this.setVelocity(0.0D, -3.0D, 0.0D);
                }
            } else if (this.fallingAttackProgress < FALLING_ATTACK_END_TICKS) {
                this.fallingAttackProgress++;
            } else if (this.isUsingFallingAttack()) {
                this.stopFallingAttack();
            }
        }
    }

    protected int computeFallDamage(float fallDistance, float damageMultiplier) {
        int damage = super.computeFallDamage(fallDistance, damageMultiplier);
        return this.isUsingFallingAttack() ? MathHelper.floor(damage * 0.1F) : damage;
    }

    protected float computeFallingAttackDistance() {
        return MathHelper.clamp(this.yPosWhenStartFallingAttack - (float) this.getY(), 0.0F, Float.MAX_VALUE);
    }

    protected float computeFallingAttackDamage(float distanceToTarget, int fallingAttackEnchantmentLevel) {
        float damage = (this.computeFallingAttackDistance() - distanceToTarget) * 0.1F * fallingAttackEnchantmentLevel;
        return MathHelper.clamp(damage, 0.0F, Float.MAX_VALUE);
    }

    protected float computeKnockbackStrength(float distanceToTarget, int fallingAttackEnchantmentLevel) {
        return MathHelper.clamp((this.computeFallingAttackDistance() - distanceToTarget) * 0.025F * fallingAttackEnchantmentLevel, 0.0F, Float.MAX_VALUE);
    }

    public boolean checkFallingAttack() {
        Box box = this.getBoundingBox();
        return this.fallingAttackCooldown <= 0 && this.world.isSpaceEmpty(this, new Box(box.minX, box.minY - 2.0D, box.minZ, box.maxX, box.maxY, box.maxZ)) && !this.isClimbing() && !this.hasPassengers() && !this.abilities.flying && !this.hasNoGravity() && !this.onGround && !this.isUsingFallingAttack() && !this.isInLava() && !this.isTouchingWater() && !this.hasStatusEffect(StatusEffects.LEVITATION) && this.getMainHandStack().getItem() instanceof SwordItem;
    }

    public void startFallingAttack() {
        this.fallingAttack = true;

        if (this.isFallFlying()) {
            this.stopFallFlying();
        }
    }

    @Inject(method = "startFallFlying", at = @At("HEAD"), cancellable = true)
    private void startFallFlying(CallbackInfo ci) {
        if (this.fallingAttack) {
            ci.cancel();
        }
    }

    public void stopFallingAttack() {
        this.fallingAttack = false;
        this.fallingAttackProgress = 0;
        this.fallingAttackCooldown = 5;
        this.yPosWhenStartFallingAttack = 0.0F;
        this.roll = 0;
    }

    public int getFallingAttackProgress() {
        return this.fallingAttackProgress;
    }

    public void setFallingAttackProgress(int fallingAttackProgress) {
        this.fallingAttackProgress = fallingAttackProgress;
    }

    public float getFallingAttackYPos() {
        return this.yPosWhenStartFallingAttack;
    }

    public void setFallingAttackYPos(float yPos) {
        this.yPosWhenStartFallingAttack = yPos;
    }

    public boolean isUsingFallingAttack() {
        return this.fallingAttack;
    }

    public float getYawF() {
        return this.storeYaw;
    }

    public void setYawF(float yaw) {
        this.storeYaw = yaw;
    }
}
