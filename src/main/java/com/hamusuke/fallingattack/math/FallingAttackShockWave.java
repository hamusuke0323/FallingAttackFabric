package com.hamusuke.fallingattack.math;

import com.google.common.collect.Lists;
import com.hamusuke.fallingattack.FallingAttack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FallingAttackShockWave {
    private final ServerPlayerEntity player;
    private final Vec3d pos;
    private final Box box;
    private final ServerWorld world;
    private final Circle primary;
    private final Circle secondary;
    private final List<LivingEntity> exceptEntities = Lists.newArrayList();
    private final BiConsumer<Entity, Float> attackFunc;
    private boolean isDead;

    public FallingAttackShockWave(ServerPlayerEntity player, Box box, BiConsumer<Entity, Float> attackFunc) {
        this.player = player;
        Vec3d pos = this.player.getPos();
        this.pos = new Vec3d(pos.x, pos.y, pos.z);
        this.box = box;
        this.world = this.player.getServerWorld();
        this.primary = new Circle(this.pos.getX(), this.pos.getZ(), 0.0D);
        this.secondary = new Circle(this.pos.getX(), this.pos.getZ(), -MathHelper.SQUARE_ROOT_OF_TWO * FallingAttack.SHOCK_WAVE_SPREADING_SPEED);
        this.attackFunc = attackFunc;
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
                this.attackFunc.accept(livingEntity, (float) ((this.box.getXLength() - this.primary.getRadius()) / this.box.getXLength()));
                this.addExceptEntity(livingEntity);
            });

            if (this.primary.getRadius() >= this.box.getXLength()) {
                this.isDead = true;
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
            boolean flag = !this.exceptEntities.contains(livingEntity) && !livingEntity.isSpectator() && livingEntity != this.player && d >= this.secondary.getRadius() && this.primary.getRadius() >= d;

            for (int i = 0; i < 2 && flag; i++) {
                Vec3d vec3d = new Vec3d(livingEntity.getX(), livingEntity.getBodyY(0.5D * (double) i), livingEntity.getZ());
                HitResult hitResult = this.world.raycast(new RaycastContext(this.pos, vec3d, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this.player));
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

    public void addExceptEntity(LivingEntity livingEntity) {
        this.exceptEntities.add(livingEntity);
    }

    public Circle getPrimary() {
        return this.primary;
    }
}
