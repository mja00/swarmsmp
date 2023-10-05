package dev.mja00.swarmsmps2.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ZombieHorse.class)
public abstract class MixinZombieHorse extends AbstractHorse {
    protected MixinZombieHorse(EntityType<? extends AbstractHorse> p_30531_, Level p_30532_) {
        super(p_30531_, p_30532_);
    }

    @Unique
    protected @NotNull SoundEvent getSwimSound() {
        if (this.onGround) {
            if (!this.isVehicle()) {
                return SoundEvents.SKELETON_HORSE_STEP_WATER;
            }

            ++this.gallopSoundCounter;
            if (this.gallopSoundCounter > 5 && this.gallopSoundCounter % 3 == 0) {
                return SoundEvents.SKELETON_HORSE_GALLOP_WATER;
            }

            if (this.gallopSoundCounter <= 5) {
                return SoundEvents.SKELETON_HORSE_STEP_WATER;
            }
        }

        return SoundEvents.SKELETON_HORSE_SWIM;
    }

    @Unique
    protected void playSwimSound(float pVolume) {
        if (this.onGround) {
            super.playSwimSound(0.3F);
        } else {
            super.playSwimSound(Math.min(0.1F, pVolume * 25.0F));
        }

    }

    @Unique
    protected void playJumpSound() {
        if (this.isInWater()) {
            this.playSound(SoundEvents.SKELETON_HORSE_JUMP_WATER, 0.4F, 1.0F);
        } else {
            super.playJumpSound();
        }

    }

    @Unique
    public double getPassengersRidingOffset() {
        return super.getPassengersRidingOffset() - 0.1875D;
    }

    @Unique
    public boolean rideableUnderWater() {
        return true;
    }

    @Unique
    protected float getWaterSlowDown() {
        return 0.86F;
    }

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    public void mobInteract(Player pPlayer, InteractionHand pHand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        // Check if the player is not on the team named "undead"
        if (pPlayer.getTeam() == null || !pPlayer.getTeam().getName().equals("undead")) {
            this.makeMad();
            cir.setReturnValue(InteractionResult.sidedSuccess(this.level.isClientSide));
            return;
        }
        if (!this.isTamed()) {
            this.makeMad();
            this.doPlayerRide(pPlayer);
            cir.setReturnValue(InteractionResult.sidedSuccess(this.level.isClientSide));
        } else if (this.isBaby()) {
            cir.setReturnValue(super.mobInteract(pPlayer, pHand));
        } else if (pPlayer.isSecondaryUseActive()) {
            this.openInventory(pPlayer);
            cir.setReturnValue(InteractionResult.sidedSuccess(this.level.isClientSide));
        } else if (this.isVehicle()) {
            cir.setReturnValue(super.mobInteract(pPlayer, pHand));
        } else {
            if (!itemstack.isEmpty()) {
                if (itemstack.is(Items.SADDLE) && !this.isSaddled()) {
                    this.openInventory(pPlayer);
                    cir.setReturnValue(InteractionResult.sidedSuccess(this.level.isClientSide));
                }

                InteractionResult interactionresult = itemstack.interactLivingEntity(pPlayer, this, pHand);
                if (interactionresult.consumesAction()) {
                    cir.setReturnValue(interactionresult);
                }
            }

            this.doPlayerRide(pPlayer);
            cir.setReturnValue(InteractionResult.sidedSuccess(this.level.isClientSide));
        }
    }
}
