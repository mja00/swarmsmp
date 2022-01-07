package dev.mja00.swarmsmp.events;

import dev.mja00.swarmsmp.SSMPS2;
import dev.mja00.swarmsmp.commands.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;

@Mod.EventBusSubscriber(modid = SSMPS2.MOD_ID)
public class ModEvents {

    static Logger LOGGER = SSMPS2.LOGGER;

    @SuppressWarnings("InstantiationOfUtilityClass")
    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        new CharmCommand(event.getDispatcher());
        new AdminCommand(event.getDispatcher());
        new DuelCommand(event.getDispatcher());
        new BetterMessageCommand(event.getDispatcher());
        new OOCCommand(event.getDispatcher());
        new BetterMeCommand(event.getDispatcher());

        ConfigCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerCloneEvent(PlayerEvent.Clone event) {
        if(!event.getOriginal().getEntityWorld().isRemote) {
            event.getOriginal().getPersistentData().keySet().forEach(key -> {
                if(key.contains(SSMPS2.MOD_ID)) {
                    event.getPlayer().getPersistentData().put(key, event.getOriginal().getPersistentData().get(key));
                }
            });
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
                    ServerPlayerEntity source = request.source;
                    ServerPlayerEntity target = request.target;

                    if (source != null) {
                        source.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "duel.timeout"), source.getUniqueID());
                    }

                    if (target != null) {
                        target.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "duel.timeout"), target.getUniqueID());
                    }

                    iterator.remove();
                }
            }
        }
    }

    // Event for whispering
    @SubscribeEvent
    public static void messageServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long now = System.currentTimeMillis();

            Iterator<BetterMessageCommand.MessageRequest> iterator = BetterMessageCommand.MessageQueue.values().iterator();

            while (iterator.hasNext()) {
                BetterMessageCommand.MessageRequest request = iterator.next();

                if (now > request.sendTime) {
                    ServerPlayerEntity sender = request.sender;
                    ServerPlayerEntity receiver = request.receiver;
                    ITextComponent message = request.message;

                    if (sender != null && receiver != null) {
                        receiver.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.message.received", sender.getDisplayName(), message).mergeStyle(TextFormatting.AQUA), receiver.getUniqueID());
                        // Also play sound when receiving message
                        BlockPos pos = receiver.getPosition();
                        receiver.getServerWorld().playSound(null, pos, SoundEvents.ENTITY_VILLAGER_WORK_CARTOGRAPHER, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    }

                    iterator.remove();
                }
            }
        }
    }
}
