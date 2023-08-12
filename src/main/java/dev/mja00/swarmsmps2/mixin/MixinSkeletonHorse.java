package dev.mja00.swarmsmps2.mixin;

import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.SkeletonTrapGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkeletonHorse.class)
public class MixinSkeletonHorse {

    private GoalSelector goalSelector;
    private SkeletonTrapGoal skeletonTrapGoal;

    @Inject(method = "setTrap", at = @At("HEAD"), cancellable = true)
    private void setTrap(boolean pTrap, CallbackInfo info) {
        // We'll just yeet the goal selector
        if (this.goalSelector == null) {
            info.cancel();
            return;
        }
        this.goalSelector.removeGoal(this.skeletonTrapGoal);
        info.cancel();
    }
}
