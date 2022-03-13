package com.hamusuke.fallingattack.math;

public record Vec2d(double x, double y) {
    public Vec2d add(double amount) {
        return new Vec2d(this.x + amount, this.y + amount);
    }
}
