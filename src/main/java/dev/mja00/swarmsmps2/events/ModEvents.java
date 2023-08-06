package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.commands.*;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class ModEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    private static final String translationKey = SwarmsmpS2.translationKey;

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new AdminCommand(event.getDispatcher());
        new BetterMeCommand(event.getDispatcher());
        new BetterMessageCommand(event.getDispatcher());
        new DuelCommand(event.getDispatcher());
        new OOCCommand(event.getDispatcher());
        new HeadCommand(event.getDispatcher());
        new JoinCommand(event.getDispatcher());
        new VerifyCommand(event.getDispatcher());
        new AliasCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerCloneEvent(PlayerEvent.Clone event) {
        if (!event.getOriginal().getLevel().isClientSide) {
            event.getOriginal().getPersistentData().getAllKeys().forEach(key -> {
               if(key.contains(SwarmsmpS2.MODID)) {
                   event.getPlayer().getPersistentData().put(key, Objects.requireNonNull(event.getOriginal().getPersistentData().get(key)));
               }
            });
        }
    }

    // Event for whispers
    @SubscribeEvent
    public static void messageServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long now = System.currentTimeMillis();

            Iterator<BetterMessageCommand.MessageRequest> iterator = BetterMessageCommand.MessageQueue.values().iterator();

            while (iterator.hasNext()) {
                BetterMessageCommand.MessageRequest request = iterator.next();

                if (now > request.sendTime) {
                    ServerPlayer sender = request.sender;
                    ServerPlayer recipient = request.recipient;
                    TextComponent message = request.message;

                    if (sender != null && recipient != null && message != null) {
                        recipient.sendMessage(new TranslatableComponent(translationKey + "commands.message.received", sender.getDisplayName(), message).withStyle(ChatFormatting.AQUA), sender.getUUID());

                        // Send the player a sound
                        BlockPos pos = recipient.getOnPos();
                        recipient.getLevel().playSound(null, pos, SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 1.0F, 1.0F);
                    }

                    iterator.remove();
                }
            }
        }
    }

    // Event for dueling
    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long now = System.currentTimeMillis();
            long timeTillExpire = 1000L * 60;

            Iterator<DuelCommand.DuelRequest> iterator = DuelCommand.REQUESTS.values().iterator();

            while (iterator.hasNext()) {
                DuelCommand.DuelRequest request = iterator.next();

                if (now > request.created + timeTillExpire) {
                    ServerPlayer source = request.source;
                    ServerPlayer target = request.target;

                    if (source != null) {
                        source.sendMessage(new TranslatableComponent(translationKey + "duel.timeout"), DUMMY);
                    }

                    if (target != null) {
                        target.sendMessage(new TranslatableComponent(translationKey + "duel.timeout"), DUMMY);
                    }

                    iterator.remove();
                }
            }
        }
    }

    // Do anvil blocking changes
    @SubscribeEvent
    public static void onAnvilChange(AnvilUpdateEvent event) {
        // We want to get the output and lefthand item
        ItemStack output = event.getRight();
        ItemStack left = event.getLeft();
        // Check if the output has enchants, if not return
        Map<Enchantment, Integer> outputMap = EnchantmentHelper.getEnchantments(output);
        if (outputMap.size() == 0) {
            return;
        }
        // So we've got an item with enchants, let's see if the left item also has enchants
        Map<Enchantment, Integer> leftMap = EnchantmentHelper.getEnchantments(left);
        if (leftMap.size() == 0) {
            // We'll also return since the user isn't combining enchants
            return;
        }
        // So there's a chance the user is trying to combine enchants to see if they can get a better enchant
        // We'll want to check if the output has an enchant 1 level higher than the left item, if it does, we cancel the event
        for (Enchantment enchant : outputMap.keySet()) {
            // Get the level of the enchant
            int level = outputMap.get(enchant);
            // Check if the left item has the enchant
            if (leftMap.containsKey(enchant)) {
                // Get the level of the left item's enchant
                int leftLevel = leftMap.get(enchant);
                // Check if the output's enchant is the same as the left item's enchant
                if (level == leftLevel) {
                    // Cancel the event
                    event.setCanceled(true);
                    // Inform the user
                    LOGGER.info("Anvil event cancelled for " + event.getPlayer().getName().getString() + " because they were trying to combine enchants.");
                    // Return
                    return;
                }
            }
        }
    }
}
