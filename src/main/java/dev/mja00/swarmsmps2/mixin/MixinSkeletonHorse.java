package dev.mja00.swarmsmps2.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.SkeletonTrapGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(SkeletonHorse.class)
public abstract class MixinSkeletonHorse extends AbstractHorse {

    private GoalSelector goalSelector;
    private SkeletonTrapGoal skeletonTrapGoal;

    protected MixinSkeletonHorse(EntityType<? extends AbstractHorse> p_30531_, Level p_30532_) {
        super(p_30531_, p_30532_);
    }

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

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
        // If we're not in a taiga biome, don't spawn
        // Get the current biome
        Biome biome = pLevel.getBiome(this.blockPosition()).value();
        // Check if the biome is a taiga biome
        if (biome != ForgeRegistries.BIOMES.getValue(Biomes.TAIGA.getRegistryName())) {
            // Don't spawn, aka kill the FUCK outta it
            this.actuallyHurt(DamageSource.ANVIL, 10000);
        }
        return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
    }
}
