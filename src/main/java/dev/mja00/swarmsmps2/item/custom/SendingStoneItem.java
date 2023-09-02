package dev.mja00.swarmsmps2.item.custom;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.client.gui.screen.ScreenOpener;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SendingStoneItem extends Item {

    public SendingStoneItem(Properties pProperties) {
        super(pProperties);
    }


    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        // If it isn't the main hand, do nothing
        if (pUsedHand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
        }
        // Get the player's offhand
        ItemStack offhand = pPlayer.getItemInHand(InteractionHand.OFF_HAND);
        Item offhandItem = offhand.getItem();
        ItemStack mainhand = pPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        Item mainhandItem = mainhand.getItem();
        // If both hands are unpaired and both are sending stones, pair them
        if ((isStone(mainhandItem) && isStone(offhandItem)) && (!isPaired(mainhand) && !isPaired(offhand))) {
            if (doActionIfNotCrouched(pLevel, pPlayer))
                return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
            if (!pLevel.isClientSide) {
                // We want to generate a random UUID and add it to both item's NBT data, under a paired value
                // This will allow us to identify which stones are paired
                // Create a new UUID
                UUID uuid = UUID.randomUUID();
                updateItem(mainhand, uuid);
                updateItem(offhand, uuid);
            }
            pPlayer.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.sidedSuccess(pPlayer.getItemInHand(pUsedHand), pLevel.isClientSide());
        }
        // If both hands are sending stones, but one is paired and the other isn't, pair the unpaired stone to the paired stone
        if (isStone(mainhandItem) && isStone(offhandItem) && (!isPaired(mainhand) || !isPaired(offhand))) {
            ItemStack unpairedStone = isPaired(mainhand) ? offhand : mainhand;
            ItemStack pairedStone = isPaired(mainhand) ? mainhand : offhand;

            if (doActionIfNotCrouched(pLevel, pPlayer))
                return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
            if (!pLevel.isClientSide) {
                if (pairedStone.getTag() == null) {
                    // Do nothing
                    return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
                }
                // Get the UUID from the paired stone
                String uuid = pairedStone.getTag().getString(SwarmsmpS2.MODID + ":paired");
                // Update the unpaired stone with the paired stone's UUID
                updateItem(unpairedStone, UUID.fromString(uuid));
            }
            pPlayer.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.sidedSuccess(pPlayer.getItemInHand(pUsedHand), pLevel.isClientSide());
        }
        // At this point we just need to see if the stone is paired
        if (isPaired(mainhand)) {
            if (isUnderCooldown(mainhand)) {
                pLevel.playSound((Player) null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.BASALT_HIT, SoundSource.NEUTRAL, 0.5F, 0.4F / (pLevel.getRandom().nextFloat() * 0.4F + 0.8F));
                // Play some particle effects
                this.addParticlesAroundSelf(ParticleTypes.ELECTRIC_SPARK, pLevel, pPlayer, 20);
                pPlayer.displayClientMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "message.item.sending_stone.cooldown").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
            } else {
                // Open our GUI
                if (pLevel.isClientSide) {
                    ScreenOpener.openSendingStoneScreen(pPlayer, mainhand);
                }
                return InteractionResultHolder.sidedSuccess(pPlayer.getItemInHand(pUsedHand), pLevel.isClientSide());
            }
        }
        return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
    }

    private boolean doActionIfNotCrouched(Level pLevel, Player pPlayer) {
        if (!pPlayer.isCrouching()) {
            // Do nothing
            return true;
        }

        pLevel.playSound((Player) null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 0.5F, 0.4F / (pLevel.getRandom().nextFloat() * 0.4F + 0.8F));
        pPlayer.displayClientMessage(new TranslatableComponent(SwarmsmpS2.translationKey + "message.item.sending_stone").withStyle(ChatFormatting.DARK_AQUA), true);
        return false;
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        // If they have the paired NBT data, make it shiny
        if (pStack.getTag() == null) return false;
        return pStack.hasTag() && pStack.getTag().contains(SwarmsmpS2.MODID + ":paired");
    }

    private static void updateItem(ItemStack item, UUID uuid) {
        item.getOrCreateTag().putString(SwarmsmpS2.MODID + ":paired", uuid.toString());
        // Update its name to attuned sending stone
        item.setHoverName(new TranslatableComponent(SwarmsmpS2.translationKey + "item.sending_stone.attuned").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        if (this.isPaired(pStack)) {
            // Get the UUID from the NBT data
            if (pStack.getTag() == null) return;
            UUID uuid = UUID.fromString(pStack.getTag().getString(SwarmsmpS2.MODID + ":paired"));
            // Truncate it to like a 6 digit number
            String uuidString = uuid.toString().substring(0, 8);
            // Add it to the tooltip
            if (Screen.hasShiftDown()) {
                // Full UUID
                pTooltipComponents.add(new TranslatableComponent(SwarmsmpS2.translationKey + "item.sending_stone.attuned_to", uuid.toString()).withStyle(ChatFormatting.DARK_AQUA));
            } else {
                pTooltipComponents.add(new TranslatableComponent(SwarmsmpS2.translationKey + "item.sending_stone.attuned_to", uuidString).withStyle(ChatFormatting.DARK_AQUA));
            }
            // We'll also add the cooldown time in a human readable form
            if (isUnderCooldown(pStack)) {
                long timeLeft = getCooldown(pStack) - System.currentTimeMillis();
                String timeLeftString = humanReadableCooldown(timeLeft);
                pTooltipComponents.add(new TranslatableComponent(SwarmsmpS2.translationKey + "item.sending_stone.cooldown_time", timeLeftString).withStyle(ChatFormatting.DARK_RED));
            }
        } else {
            // Add a tooltip telling that it's unattuned
            pTooltipComponents.add(new TranslatableComponent(SwarmsmpS2.translationKey + "item.sending_stone.unattuned").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    public boolean isPaired(ItemStack item) {
        // Get this item's NBT data
        // If it has a paired value, return true
        if (item.getTag() == null) return false;
        return item.hasTag() && item.getTag().contains(SwarmsmpS2.MODID + ":paired");
    }

    private void addParticlesAroundSelf(ParticleOptions particleOptions, Level level, Player player, int count) {
        for (int i = 0; i < count; ++i) {
            double d0 = level.getRandom().nextGaussian() * 0.02D;
            double d1 = level.getRandom().nextGaussian() * 0.02D;
            double d2 = level.getRandom().nextGaussian() * 0.02D;
            level.addParticle(particleOptions, player.getRandomX(1), player.getY() + 2, player.getRandomZ(1), d0, d1, d2);
        }
    }

    private long getCooldown(ItemStack item) {
        if (item.getTag() == null) return 0;
        return item.getTag().getLong(SwarmsmpS2.MODID + ":cooldown");
    }

    private boolean isUnderCooldown(ItemStack item) {
        if (item.getTag() == null) return false;
        // Get the cooldown
        long cooldown = getCooldown(item);
        // Get the current time
        long currentTime = System.currentTimeMillis();
        // If the cooldown is less than the current time, we're good
        return cooldown > currentTime;
    }

    private boolean isStone(Item item) {
        return item instanceof SendingStoneItem;
    }

     private String humanReadableCooldown(long cooldown) {
        // Return a HH:MM:SS string
        // Get the hours
        long hours = cooldown / 3600000;
        // Get the minutes
        long minutes = (cooldown % 3600000) / 60000;
        // Get the seconds
        long seconds = ((cooldown % 3600000) % 60000) / 1000;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
     }
}
