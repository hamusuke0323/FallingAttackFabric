package com.hamusuke.fallingattack.math;

import com.google.common.collect.Lists;
import com.hamusuke.fallingattack.FallingAttack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class FallingAttackShockWave {
    private final ServerPlayerEntity owner;
    private final ItemStack sword;
    private final Vec3d pos;
    private final Box box;
    private final ServerWorld world;
    private final Circle primary;
    private final Circle secondary;
    private final List<LivingEntity> exceptEntities = Lists.newArrayList();
    private final BiFunction<Float, Integer, Float> fallingAttackDamageComputer;
    private final BiFunction<Float, Integer, Float> knockbackStrengthComputer;
    private boolean isDead;

    public FallingAttackShockWave(ServerPlayerEntity owner, ItemStack swordItem, Box box, BiFunction<Float, Integer, Float> fallingAttackDamageComputer, BiFunction<Float, Integer, Float> knockbackStrengthComputer) {
        this.owner = owner;
        this.sword = swordItem;
        this.fallingAttackDamageComputer = fallingAttackDamageComputer;
        this.knockbackStrengthComputer = knockbackStrengthComputer;
        Vec3d pos = this.owner.getPos();
        this.pos = new Vec3d(pos.x, pos.y, pos.z);
        this.box = box;
        this.world = this.owner.getWorld();
        this.primary = new Circle(this.pos.getX(), this.pos.getZ(), 0.0D);
        this.secondary = new Circle(this.pos.getX(), this.pos.getZ(), -MathHelper.SQUARE_ROOT_OF_TWO * FallingAttack.SHOCK_WAVE_SPREADING_SPEED);
    }

    static double distanceTo2D(Vec3d from, Vec3d to) {
        double x = to.x - from.x;
        double z = to.z - from.z;
        return Math.sqrt(x * x + z * z);
    }

    public void tick() {
        if (!this.isDead) {
            this.primary.spread(FallingAttack.SHOCK_WAVE_SPREADING_SPEED);
            this.forEachVec2d(vec2d -> this.world.spawnParticles(ParticleTypes.EXPLOSION.getType(), vec2d.x(), this.pos.y, vec2d.y(), 1, 1.0D, 0.0D, 0.0D, 1.0D), 6);
            this.secondary.spread(FallingAttack.SHOCK_WAVE_SPREADING_SPEED);
            this.getEntitiesStruckByShockWave().forEach(livingEntity -> {
                this.damage(livingEntity, (float) ((this.box.getXLength() - this.primary.getRadius()) / this.box.getXLength()));
                this.exceptEntities.add(livingEntity);
            });

            if (this.primary.getRadius() >= this.box.getXLength()) {
                this.isDead = true;
            }
        }
    }

    private void damage(Entity target, float damageModifier) {
        if (target.isAttackable()) {
            if (!target.handleAttack(this.owner)) {
                float damageAmount = (float) this.owner.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                float attackDamage;
                if (target instanceof LivingEntity) {
                    attackDamage = EnchantmentHelper.getAttackDamage(this.sword, ((LivingEntity) target).getGroup());
                } else {
                    attackDamage = EnchantmentHelper.getAttackDamage(this.sword, EntityGroup.DEFAULT);
                }

                if (damageAmount > 0.0F || attackDamage > 0.0F) {
                    float distanceToTarget = (float) this.pos.distanceTo(target.getPos());
                    int fallingAttackLevel = EnchantmentHelper.getLevel(FallingAttack.SHARPNESS_OF_FALLING_ATTACK, this.sword) + 1;
                    fallingAttackLevel = MathHelper.clamp(fallingAttackLevel, 1, 255);
                    attackDamage += this.fallingAttackDamageComputer.apply(distanceToTarget, fallingAttackLevel);
                    this.world.playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, this.owner.getSoundCategory(), 1.0F, 1.0F);

                    boolean bl3 = !this.owner.isClimbing() && !this.owner.isTouchingWater() && !this.owner.hasStatusEffect(StatusEffects.BLINDNESS) && !this.owner.hasVehicle() && target instanceof LivingEntity;
                    if (bl3) {
                        damageAmount *= 1.5F;
                    }

                    damageAmount += attackDamage;
                    float targetHealth = 0.0F;
                    boolean fireAspectEnchanted = false;
                    int fireAspectLevel = EnchantmentHelper.getLevel(Enchantments.FIRE_ASPECT, this.sword);
                    if (target instanceof LivingEntity) {
                        targetHealth = ((LivingEntity) target).getHealth();
                        if (fireAspectLevel > 0 && !target.isOnFire()) {
                            fireAspectEnchanted = true;
                            target.setOnFireFor(1);
                        }
                    }

                    Vec3d vec3d = target.getVelocity();
                    boolean tookDamage = target.damage(DamageSource.player(this.owner), damageAmount * damageModifier);
                    if (tookDamage) {
                        float yaw = (float) -MathHelper.atan2(target.getX() - this.pos.getX(), target.getZ() - this.pos.getZ());
                        float strength = this.knockbackStrengthComputer.apply(distanceToTarget, fallingAttackLevel);
                        if (target instanceof LivingEntity) {
                            ((LivingEntity) target).takeKnockback(strength, MathHelper.sin(yaw), -MathHelper.cos(yaw));
                        } else {
                            target.addVelocity(-MathHelper.sin(yaw) * strength, 0.1D, MathHelper.cos(yaw) * strength);
                        }

                        this.owner.setVelocity(this.owner.getVelocity().multiply(0.6D, 1.0D, 0.6D));
                        this.owner.setSprinting(false);

                        if (target instanceof ServerPlayerEntity && target.velocityModified) {
                            ((ServerPlayerEntity) target).networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(target));
                            target.velocityModified = false;
                            target.setVelocity(vec3d);
                        }

                        if (bl3) {
                            this.world.playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, this.owner.getSoundCategory(), 1.0F, 1.0F);
                            this.owner.addCritParticles(target);
                        }

                        if (attackDamage > 0.0F) {
                            this.owner.addEnchantedHitParticles(target);
                        }

                        this.owner.onAttacking(target);

                        if (target instanceof LivingEntity) {
                            float n = targetHealth - ((LivingEntity) target).getHealth();
                            this.owner.increaseStat(Stats.DAMAGE_DEALT, Math.round(n * 10.0F));
                            if (fireAspectLevel > 0) {
                                target.setOnFireFor(fireAspectLevel * 4);
                            }

                            if (n > 2.0F) {
                                int o = (int) ((double) n * 0.5D);
                                this.world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getBodyY(0.5D), target.getZ(), o, 0.1D, 0.0D, 0.1D, 0.2D);
                            }
                        }
                    } else {
                        this.world.playSound(null, this.owner.getX(), this.owner.getY(), this.owner.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, this.owner.getSoundCategory(), 1.0F, 1.0F);
                        if (fireAspectEnchanted) {
                            target.extinguish();
                        }
                    }
                }
            }
        }
    }

    public void forEachVec2d(Consumer<Vec2d> consumer, int slices) {
        for (int i = 0; i < 360; i++) {
            if (i % slices == 0) {
                consumer.accept(this.primary.getCoordinates((float) (i * Math.PI / 180.0F), true));
            }
        }
    }

    public List<LivingEntity> getEntitiesStruckByShockWave() {
        return this.world.getEntitiesByClass(LivingEntity.class, this.box, livingEntity -> {
            double d = distanceTo2D(livingEntity.getPos(), this.pos);
            boolean flag = !this.exceptEntities.contains(livingEntity) && !livingEntity.isSpectator() && livingEntity != this.owner && d >= this.secondary.getRadius() && this.primary.getRadius() >= d;

            for (int i = 0; i < 2 && flag; i++) {
                Vec3d vec3d = new Vec3d(livingEntity.getX(), livingEntity.getBodyY(0.5D * (double) i), livingEntity.getZ());
                HitResult hitResult = this.world.raycast(new RaycastContext(this.pos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this.owner));
                if (hitResult.getType() == HitResult.Type.MISS) {
                    return true;
                }
            }

            return false;
        });
    }

    public boolean isDead() {
        return this.isDead;
    }
}
