package dev.mja00.swarmsmps2.item.custom;

import dev.mja00.swarmsmps2.helpers.EntityHelpers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public class FaePoofItem extends Item {

        public FaePoofItem(Properties pProperties) {
            super(pProperties);
        }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        // Check if the player is in creative
        if (pPlayer.isCreative()) {
            // If they are in creative we'll do some fun things :)
            // Get a server level
            if (!pLevel.isClientSide()) {
                ServerLevel serverLevel = (ServerLevel) pLevel;
                EntityHelpers.addParticlesAroundSelfServer(ParticleTypes.POOF, serverLevel, pPlayer, 500, 3);
                // Play a sound
                serverLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.5F, 0.4F / (pLevel.getRandom().nextFloat() * 0.4F + 0.8F));
            }

            if (!pLevel.isClientSide()) {
                // Set the player to spectator
                ServerPlayer player = (ServerPlayer) pPlayer;
                player.setGameMode(GameType.SPECTATOR);
            }
            return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
        }
        return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
    }
}
