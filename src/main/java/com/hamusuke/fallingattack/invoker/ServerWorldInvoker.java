package com.hamusuke.fallingattack.invoker;

import com.hamusuke.fallingattack.math.FallingAttackShockWave;

public interface ServerWorldInvoker {
    default void summonShockWave(FallingAttackShockWave shockWave) {
    }
}
