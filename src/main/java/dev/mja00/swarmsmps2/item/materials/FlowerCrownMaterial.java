package dev.mja00.swarmsmps2.item.materials;

import dev.mja00.swarmsmps2.item.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public class FlowerCrownMaterial implements ArmorMaterial {
    public static final FlowerCrownMaterial INSTANCE = new FlowerCrownMaterial();

    @Override
    public int getDurabilityForSlot(EquipmentSlot slot) {
        return 64;
    }

    @Override
    public int getDefenseForSlot(EquipmentSlot slot) {
        return 0;
    }

    @Override
    public int getEnchantmentValue() {
        return 64;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.AZALEA_FLOWER.get());
    }

    @Override
    public String getName() {
        return "flower";
    }

    @Override
    public float getToughness() {
        return 0f;
    }

    @Override
    public float getKnockbackResistance() {
        return 0;
    }
}
