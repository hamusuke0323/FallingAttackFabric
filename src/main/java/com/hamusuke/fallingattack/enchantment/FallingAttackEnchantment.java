package com.hamusuke.fallingattack.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class FallingAttackEnchantment extends Enchantment {
    public FallingAttackEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    public int getMaxLevel() {
        return 5;
    }
}
