package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.commands.AdminCommand;
import dev.mja00.swarmsmps2.commands.BetterMeCommand;
import dev.mja00.swarmsmps2.commands.BetterMessageCommand;
import dev.mja00.swarmsmps2.commands.DuelCommand;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
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
}
