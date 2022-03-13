package com.hamusuke.fallingattack.math;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.MathHelper;

public class Circle {
    private final Vec2d center;
    private double radius;

    public Circle(double centerX, double centerY, double radius) {
        this.center = new Vec2d(centerX, centerY);
        this.radius = radius;
    }

    public static Circle unpack(PacketByteBuf byteBuf) {
        return new Circle(byteBuf.readDouble(), byteBuf.readDouble(), byteBuf.readDouble());
    }

    public void spread(double amount) {
        this.radius += amount;
    }

    public Vec2d getCoordinates(float angRad, boolean translate) {
        return new Vec2d(this.radius * MathHelper.cos(angRad) + (translate ? this.center.x() : 0.0D), this.radius * MathHelper.sin(angRad) + (translate ? this.center.y() : 0.0D));
    }

    public Vec2d getCenter() {
        return this.center;
    }

    public double getRadius() {
        return this.radius;
    }

    public PacketByteBuf pack(PacketByteBuf byteBuf) {
        byteBuf.writeDouble(this.center.x()).writeDouble(this.center.y()).writeDouble(this.radius);
        return byteBuf;
    }
}
