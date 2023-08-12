package dev.mja00.swarmsmps2.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {

    @Shadow public abstract float getMaxHealth();

    @Shadow public abstract float getHealth();

    @Shadow public abstract void setHealth(float newHealth);

    @Unique
    @Nullable
    private Float swarmsmp_s2$actualHealth = null;

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void swarmsmp_s2$readAdditionalSaveData(CompoundTag tag, CallbackInfo callback) {
        if (tag.contains("Health", Tag.TAG_ANY_NUMERIC)) {
            final float savedHealth = tag.getFloat("Health");
            if (savedHealth > getMaxHealth() && savedHealth > 0) {
                swarmsmp_s2$actualHealth = savedHealth;
            }
        }
    }

    @Inject(method = "detectEquipmentUpdates", at = @At("RETURN"))
    private void swarmsmp_s2$detectEquipmentUpdates(CallbackInfo callback) {
        if (swarmsmp_s2$actualHealth != null) {
            if (swarmsmp_s2$actualHealth > 0 && swarmsmp_s2$actualHealth > this.getHealth()) {
                this.setHealth(swarmsmp_s2$actualHealth);
            }
            swarmsmp_s2$actualHealth = null;
        }
    }
}
