package com.hamusuke.fallingattack.mixin;

import com.hamusuke.fallingattack.FallingAttack;
import com.hamusuke.fallingattack.invoker.PlayerEntityInvoker;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
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
    public abstract void addEnchantedHitParticles(Entity target);

    @Shadow
    public abstract void resetLastAttackedTicks();

    @Shadow
    public abstract void addCritParticles(Entity target);

    @Shadow
    public abstract void addExhaustion(float exhaustion);

    @Shadow
    public abstract void increaseStat(Identifier stat, int amount);

    @Shadow
    public abstract void stopFallFlying();

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
                    this.setVelocity(new Vec3d(0.0D, 0.5D, 0.0D));
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
                    if (!this.world.isClient() && EnchantmentHelper.getLevel(FallingAttack.FALLING_ATTACK, this.getMainHandStack()) > 0) {
                        Box box = this.getBoundingBox().expand(3.0D, 0.0D, 3.0D);
                        Vec3d vec3d = this.getPos();

                        this.world.getEntitiesByClass(LivingEntity.class, new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY - 1.0D, box.maxZ), livingEntity -> {
                            boolean flag = !livingEntity.isSpectator() && livingEntity != this;

                            for (int i = 0; i < 2 && flag; i++) {
                                Vec3d vec3d1 = new Vec3d(livingEntity.getX(), livingEntity.getBodyY(0.5D * (double) i), livingEntity.getZ());
                                HitResult hitResult = this.world.raycast(new RaycastContext(vec3d, vec3d1, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
                                if (hitResult.getType() == HitResult.Type.MISS) {
                                    return true;
                                }
                            }

                            return false;
                        }).forEach(this::fallingAttack);
                    }
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
        int level = EnchantmentHelper.getEquipmentLevel(FallingAttack.FALLING_ATTACK, this);
        return this.isUsingFallingAttack() && level > 0 ? (int) (damage * (0.5F / level)) : damage;
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

    public void fallingAttack(Entity target) {
        if (target.isAttackable()) {
            if (!target.handleAttack(this)) {
                float damageAmount = (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                float attackDamage;
                if (target instanceof LivingEntity) {
                    attackDamage = EnchantmentHelper.getAttackDamage(this.getMainHandStack(), ((LivingEntity) target).getGroup());
                } else {
                    attackDamage = EnchantmentHelper.getAttackDamage(this.getMainHandStack(), EntityGroup.DEFAULT);
                }

                this.resetLastAttackedTicks();
                if (damageAmount > 0.0F || attackDamage > 0.0F) {
                    float distanceToTarget = this.distanceTo(target);
                    int fallingAttackLevel = EnchantmentHelper.getLevel(FallingAttack.FALLING_ATTACK, this.getMainHandStack());
                    attackDamage += this.computeFallingAttackDamage(distanceToTarget, fallingAttackLevel);
                    this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, this.getSoundCategory(), 1.0F, 1.0F);
                    ++fallingAttackLevel;

                    boolean bl3 = !this.isClimbing() && !this.isTouchingWater() && !this.hasStatusEffect(StatusEffects.BLINDNESS) && !this.hasVehicle() && target instanceof LivingEntity;
                    if (bl3) {
                        damageAmount *= 1.5F;
                    }

                    damageAmount += attackDamage;
                    float targetHealth = 0.0F;
                    boolean fireAspectEnchanted = false;
                    int fireAspectLevel = EnchantmentHelper.getFireAspect(this);
                    if (target instanceof LivingEntity) {
                        targetHealth = ((LivingEntity) target).getHealth();
                        if (fireAspectLevel > 0 && !target.isOnFire()) {
                            fireAspectEnchanted = true;
                            target.setOnFireFor(1);
                        }
                    }

                    Vec3d vec3d = target.getVelocity();
                    boolean tookDamage = target.damage(DamageSource.player((PlayerEntity) (Object) this), damageAmount);
                    if (tookDamage) {
                        if (fallingAttackLevel > 0) {
                            float yaw = (float) -MathHelper.atan2(target.getX() - this.getX(), target.getZ() - this.getZ()) * 57.2957795F;
                            float strength = this.computeKnockbackStrength(distanceToTarget, fallingAttackLevel);
                            if (target instanceof LivingEntity) {
                                ((LivingEntity) target).takeKnockback(strength, MathHelper.sin(yaw * 0.017453292F), -MathHelper.cos(yaw * 0.017453292F));
                            } else {
                                target.addVelocity(-MathHelper.sin(yaw * 0.017453292F) * strength, 0.1D, MathHelper.cos(yaw * 0.017453292F) * strength);
                            }

                            this.setVelocity(this.getVelocity().multiply(0.6D, 1.0D, 0.6D));
                            this.setSprinting(false);
                        }

                        if (target instanceof ServerPlayerEntity && target.velocityModified) {
                            ((ServerPlayerEntity) target).networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(target));
                            target.velocityModified = false;
                            target.setVelocity(vec3d);
                        }

                        if (bl3) {
                            this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, this.getSoundCategory(), 1.0F, 1.0F);
                            this.addCritParticles(target);
                        }

                        if (attackDamage > 0.0F) {
                            this.addEnchantedHitParticles(target);
                        }

                        this.onAttacking(target);
                        if (target instanceof LivingEntity) {
                            EnchantmentHelper.onUserDamaged((LivingEntity) target, this);
                        }

                        EnchantmentHelper.onTargetDamaged(this, target);
                        ItemStack itemStack2 = this.getMainHandStack();
                        Entity entity = target;
                        if (target instanceof EnderDragonPart) {
                            entity = ((EnderDragonPart) target).owner;
                        }

                        if (!this.world.isClient && !itemStack2.isEmpty() && entity instanceof LivingEntity) {
                            itemStack2.postHit((LivingEntity) entity, (PlayerEntity) (Object) this);
                            if (itemStack2.isEmpty()) {
                                this.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                            }
                        }

                        if (target instanceof LivingEntity) {
                            float n = targetHealth - ((LivingEntity) target).getHealth();
                            this.increaseStat(Stats.DAMAGE_DEALT, Math.round(n * 10.0F));
                            if (fireAspectLevel > 0) {
                                target.setOnFireFor(fireAspectLevel * 4);
                            }

                            if (this.world instanceof ServerWorld && n > 2.0F) {
                                int o = (int) ((double) n * 0.5D);
                                ((ServerWorld) this.world).spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getBodyY(0.5D), target.getZ(), o, 0.1D, 0.0D, 0.1D, 0.2D);
                            }
                        }

                        this.addExhaustion(0.1F);
                    } else {
                        this.world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, this.getSoundCategory(), 1.0F, 1.0F);
                        if (fireAspectEnchanted) {
                            target.extinguish();
                        }
                    }
                }
            }
        }
    }

    public boolean checkFallingAttack() {
        Box box = this.getBoundingBox();
        return this.fallingAttackCooldown == 0 && this.world.isSpaceEmpty(this, new Box(box.minX, box.minY - 2.0D, box.minZ, box.maxX, box.maxY, box.maxZ)) && !this.isClimbing() && !this.hasPassengers() && !this.abilities.flying && !this.hasNoGravity() && !this.onGround && !this.isUsingFallingAttack() && !this.isInLava() && !this.isTouchingWater() && !this.hasStatusEffect(StatusEffects.LEVITATION) && EnchantmentHelper.getLevel(FallingAttack.FALLING_ATTACK, this.getMainHandStack()) > 0;
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
        this.fallingAttackCooldown = 20;
        this.yPosWhenStartFallingAttack = 0.0F;
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
