package dev.mja00.swarmsmps2.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pig.class)
public abstract class MixinPig extends Animal implements ItemSteerable, Saddleable  {

    protected MixinPig(EntityType<? extends Animal> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
    }

    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning, CallbackInfo ci) {
        super.thunderHit(pLevel, pLightning);
        ci.cancel();
    }
}
