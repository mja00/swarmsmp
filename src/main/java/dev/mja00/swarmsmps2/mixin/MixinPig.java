package dev.mja00.swarmsmps2.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pig.class)
public class MixinPig extends Animal implements ItemSteerable, Saddleable  {

    protected MixinPig(EntityType<? extends Animal> p_27557_, Level p_27558_) {
        super(p_27557_, p_27558_);
    }

    @Shadow
    public AgeableMob getBreedOffspring(ServerLevel p_146743_, AgeableMob p_146744_) {
        return null;
    }
    @Shadow
    public boolean boost() {
        return false;
    }
    @Shadow
    public void travelWithInput(Vec3 pTravelVec) {

    }
    @Shadow
    public float getSteeringSpeed() {
        return 0;
    }
    @Shadow
    public boolean isSaddleable() {
        return false;
    }
    @Shadow
    public void equipSaddle(@Nullable SoundSource p_21748_) {

    }
    @Shadow
    public boolean isSaddled() {
        return false;
    }

    @Inject(method = "thunderHit", at = @At("HEAD"), cancellable = true)
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning, CallbackInfo ci) {
        super.thunderHit(pLevel, pLightning);
        ci.cancel();
    }
}
