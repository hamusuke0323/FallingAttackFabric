package com.hamusuke.fallingattack.invoker;

public interface PlayerEntityInvoker {
    int FIRST_FALLING_ATTACK_PROGRESS_TICKS = 10;
    int FALLING_ATTACK_END_TICKS = FIRST_FALLING_ATTACK_PROGRESS_TICKS + 6;

    default boolean checkFallingAttack() {
        return false;
    }

    default void startFallingAttack() {
    }

    default void stopFallingAttack() {
    }

    default int getFallingAttackProgress() {
        return 0;
    }

    default void setFallingAttackProgress(int progress) {
    }

    default float getFallingAttackYPos() {
        return 0.0F;
    }

    default void setFallingAttackYPos(float yPos) {
    }

    default boolean isUsingFallingAttack() {
        return false;
    }

    default void sendFallingAttackPacket(boolean start) {
    }

    default void sendSynchronizeFallingAttackPacket() {
    }

    default float getYawF() {
        return 0.0F;
    }

    default void setYawF(float yaw) {
    }
}
