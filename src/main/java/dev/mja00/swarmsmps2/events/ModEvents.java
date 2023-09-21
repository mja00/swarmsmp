package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.commands.*;
import dev.mja00.swarmsmps2.helpers.WeightedWeatherEvent;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.command.ConfigCommand;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class ModEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    private static final String translationKey = SwarmsmpS2.translationKey;
    private static long lastWeatherChangeTime = 0;
    private static int nextRandomMinute = 0;
    private static Map<String, WeightedWeatherEvent<String>> weatherChances;
    // Create a class wide random
    private static final java.util.Random RANDOM = new java.util.Random();

    @SubscribeEvent
    public static void onCommandsRegister(RegisterCommandsEvent event) {
        // Get all the classes in the commands package and register them
        new AdminCommand(event.getDispatcher());
        new BetterMeCommand(event.getDispatcher());
        new BetterMessageCommand(event.getDispatcher());
        new DuelCommand(event.getDispatcher());
        new OOCCommand(event.getDispatcher());
        new HeadCommand(event.getDispatcher());
        new JoinCommand(event.getDispatcher());
        new VerifyCommand(event.getDispatcher());
        new AliasCommand(event.getDispatcher());
        new FallbackCommand(event.getDispatcher());
        new ReplyCommand(event.getDispatcher());
        new WhosOnlineCommand(event.getDispatcher());
        new RaidCommand(event.getDispatcher());
        new HungerCommand(event.getDispatcher());
        new ModCommand(event.getDispatcher());

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

    @SubscribeEvent
    public static void adminMessageServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long now = System.currentTimeMillis();
            long timeTillExpire = 1000L * 60;

            AdminCommand.MESSAGES.values().removeIf(request -> now > request.created + timeTillExpire);
        }
    }

    @SubscribeEvent
    public static void raidServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long now = System.currentTimeMillis();
            // If it's past whatever the request.expires is, remove it from the map
            RaidCommand.RAIDS.values().removeIf(request -> now > request.expires);
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
        if (outputMap.isEmpty()) {
            return;
        }
        // So we've got an item with enchants, let's see if the left item also has enchants
        Map<Enchantment, Integer> leftMap = EnchantmentHelper.getEnchantments(left);
        if (leftMap.isEmpty()) {
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

    private static Map<String, WeightedWeatherEvent<String>> getWeatherChances() {
        Map<String, WeightedWeatherEvent<String>> chances = new HashMap<>();
        // Make a weighted weather event for each weather type
        WeightedWeatherEvent<String> clear = new WeightedWeatherEvent<>();
        WeightedWeatherEvent<String> rain = new WeightedWeatherEvent<>();
        WeightedWeatherEvent<String> thunder = new WeightedWeatherEvent<>();
        // Now we add each change to event
        clear.addEntry("clear", SSMPS2Config.SERVER.clearToClearChance.get());
        clear.addEntry("rain", SSMPS2Config.SERVER.clearToRainChance.get());
        clear.addEntry("thunder", SSMPS2Config.SERVER.clearToThunderChance.get());
        rain.addEntry("clear", SSMPS2Config.SERVER.rainToClearChance.get());
        rain.addEntry("rain", SSMPS2Config.SERVER.rainToRainChance.get());
        rain.addEntry("thunder", SSMPS2Config.SERVER.rainToThunderChance.get());
        thunder.addEntry("clear", SSMPS2Config.SERVER.thunderToClearChance.get());
        thunder.addEntry("rain", SSMPS2Config.SERVER.thunderToRainChance.get());
        thunder.addEntry("thunder", SSMPS2Config.SERVER.thunderToThunderChance.get());
        // Add each event to the map
        chances.put("clear", clear);
        chances.put("rain", rain);
        chances.put("thunder", thunder);
        // Return the map
        return chances;
    }

    private static void updateWeather(ServerLevel level, String weather) {
        // Update the last weather change time
        lastWeatherChangeTime = System.currentTimeMillis() / 1000L;
        nextRandomMinute = RANDOM.nextInt(300) - 150;
        switch (weather) {
            case "clear" -> {
                // We'll want to stop the rain and thunder
                level.setWeatherParameters(0, 6000, false, false);
            }
            case "rain" -> {
                // We'll want to start the rain
                level.setWeatherParameters(0, 6000, true, false);
            }
            case "thunder" -> {
                // We'll want to start the thunder
                level.setWeatherParameters(0, 6000, true, true);
            }
        }
        SSMPS2Config.setCurrentWeather(weather);
        SSMPS2Config.setTimeOfWeatherChange(lastWeatherChangeTime);
    }

    @SubscribeEvent
    public static void chunkLoadTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side == LogicalSide.SERVER) {
            ServerLevel level = (ServerLevel) event.world;
            LongIterator forcedChunks = level.getForcedChunks().longIterator();

            while (forcedChunks.hasNext()) {
                long currentChunk = forcedChunks.nextLong();
                LevelChunk chunk = level.getChunk(ChunkPos.getX(currentChunk), ChunkPos.getZ(currentChunk));
                boolean noPlayers = level.getPlayers(p -> {
                    if (p.isSpectator()) {
                        // Skip all this math if they're spectator
                        return false;
                    }
                    // We only care about the X & Z axis due to chunks being loaded no matter the Y'
                    BlockPos playerPos = new BlockPos(p.getX(), 0, p.getZ());
                    BlockPos chunkPos = chunk.getPos().getWorldPosition();
                    double distanceToChunk = playerPos.distSqr(chunkPos);
                    // Get the server's current simulation distance
                    double simulationDistance = level.getServer().getPlayerList().getSimulationDistance() * 16;
                    // If the player is in range of the chunk return true
                    return distanceToChunk <= simulationDistance;
                }).isEmpty();

                if (noPlayers) {
                    level.tickChunk(chunk, level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onWeatherTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (event.side.isClient() || SSMPS2Config.SERVER.fallbackServer.get()) {
                return;
            }
            long lastWeatherChange = lastWeatherChangeTime == 0 ? SSMPS2Config.getTimeOfWeatherChange() : lastWeatherChangeTime;
            long now = System.currentTimeMillis() / 1000L;
            if (now - lastWeatherChange < SSMPS2Config.SERVER.weatherCheckTime.get() + nextRandomMinute) {
                return;
            }
            boolean isRaining = event.world.isRaining();
            boolean isThundering = event.world.isThundering();
            ServerLevel overworld = (ServerLevel) event.world;
            Map<String, WeightedWeatherEvent<String>> chances = weatherChances == null ? getWeatherChances() : weatherChances;
            WeightedWeatherEvent<String> selectedChance = isThundering ? chances.get("thunder") : isRaining ? chances.get("rain") : chances.get("clear");
            String newWeather = selectedChance.getRandom();
            LOGGER.info("Changing weather to " + newWeather);
            updateWeather(overworld, newWeather);
        }
    }
}
