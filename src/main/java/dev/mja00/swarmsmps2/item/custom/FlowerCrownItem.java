package dev.mja00.swarmsmps2.item.custom;

import dev.mja00.swarmsmps2.particle.ModParticles;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

public class FlowerCrownItem extends ArmorItem {

    public FlowerCrownItem(ArmorMaterial pMaterial, EquipmentSlot pSlot, Properties pProperties) {
        super(pMaterial, pSlot, pProperties);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (level.isClientSide()) {
            spawnParticles(stack, level, player);
        }
    }

    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return getModelTexture(stack);
    }

    private static void spawnParticles(ItemStack stack, Level level, Entity entity) {
        if (level.random.nextFloat() < 0.05) {
            Vec3 v = entity.getViewVector(1).scale(entity.isSwimming() ? 1.8 : -0.15f);

            level.addParticle(getParticle(stack),
                    v.x + entity.getRandomX(0.675D),
                    v.y + entity.getY() + entity.getEyeHeight() + 0.15D,
                    v.z + entity.getRandomZ(0.675D),0,0,0);
        }
    }

    @Nullable
    public static String getModelTexture(ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            var name = stack.getHoverName().getContents();
            var m = SUPPORTERS_LIST.get(name.toLowerCase(Locale.ROOT));
            if (m != null) return m.textureLocation;
        }
        return null;
    }

    public static SimpleParticleType getParticle(ItemStack stack) {
        var type = getSpecialType(stack);
        if (type != null) return type.particle.get();
        return ModParticles.AZALEA_FLOWER.get();
    }

    public static float getItemTextureIndex(ItemStack stack) {
        var type = getSpecialType(stack);
        if (type != null) return type.itemModelIndex;
        return 0f;
    }


    @Nullable
    public static SpecialType getSpecialType(ItemStack stack) {
        if (stack.hasCustomHoverName()) {
            var name = stack.getHoverName().getContents();
            return SUPPORTERS_LIST.get(name.toLowerCase(Locale.ROOT));
        }
        return null;
    }

    private static final Map<String, SpecialType> SUPPORTERS_LIST = new HashMap<>() {{
        //dev and gift crowns
        put("mja00", new SpecialType("textures/models/armor/mja00.png",
                0.10f, ModParticles.FLOWER_BEE));

        put("cogs", new SpecialType("textures/models/armor/cogs.png",
                0.20f, ModParticles.COG));

        put("roses", new SpecialType("textures/models/armor/roses.png",
                0.201f, ModParticles.ROSES));

        put("trans", new SpecialType("textures/models/armor/trans.png",
                0.202f, ModParticles.TRANS));
    }};

    public record SpecialType(String textureLocation, float itemModelIndex,
                              Supplier<SimpleParticleType> particle) {
    }

}
