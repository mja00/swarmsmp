package dev.mja00.swarmsmps2.mixin;

import dev.mja00.swarmsmps2.SSMPS2Config;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEgg.class)
public class MixinThrownEgg extends ThrowableItemProjectile {

    public MixinThrownEgg(EntityType<? extends ThrownEgg> p_37473_, Level p_37474_) {
        super(p_37473_, p_37474_);
    }

    public MixinThrownEgg(Level p_37481_, LivingEntity p_37482_) {
        super(EntityType.EGG, p_37482_, p_37481_);
    }

    public MixinThrownEgg(Level p_37476_, double p_37477_, double p_37478_, double p_37479_) {
        super(EntityType.EGG, p_37477_, p_37478_, p_37479_, p_37476_);
    }

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    public void onHit(HitResult pResult, CallbackInfo info) {
        super.onHit(pResult);
        if (!this.level.isClientSide) {
            // One in every 16 eggs will spawn a chicken
            if (this.random.nextInt(SSMPS2Config.SERVER.chickenFromEggChance.get()) == 0) {
                Chicken chicken = EntityType.CHICKEN.create(this.level);
                if (chicken == null) {
                    info.cancel();
                    return;
                }
                chicken.setAge(-24000);
                chicken.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
                this.level.addFreshEntity(chicken);
            }
            this.level.broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return Items.EGG;
    }
}
