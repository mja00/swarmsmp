package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.EntityHelpers;
import dev.mja00.swarmsmps2.helpers.SiteAPIHelper;
import dev.mja00.swarmsmps2.objects.CommandInfo;
import dev.mja00.swarmsmps2.objects.Commands;
import dev.mja00.swarmsmps2.objects.DeathEventObject;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Logger;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID, value = Dist.DEDICATED_SERVER)
public class ServerPlayerEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final String translationKey = SwarmsmpS2.translationKey;
    static final Random rnd = new Random();
    static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
    static final ExecutorService exe = Executors.newCachedThreadPool();


    private static String generateRandomString(int length) {
        // Define our characterset
        String chars = "-    "; // Either dash or a space
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String getRandomWordFromList(boolean useItems) {
        List<Item> items = ForgeRegistries.ITEMS.getValues().stream().toList();
        List<EntityType<?>> entities = ForgeRegistries.ENTITIES.getValues().stream().toList();
        if (!useItems) {
            // Get random entity
            EntityType<?> entity = entities.get(rnd.nextInt(entities.size()));
            return entity.getDescriptionId();
        } else {
            // Get random item
            Item item = items.get(rnd.nextInt(items.size()));
            return item.getDescriptionId();
        }
    }

    private static void clearChat(ServerPlayer player) {
        // Clear the player's chat
        MutableComponent chatClearer = new TextComponent(generateRandomString(68));
        for (int i = 0; i <= 121; i++) {
            if (i % 10 == 0) {
                // Append a random word
                chatClearer.append(new TranslatableComponent(getRandomWordFromList(rnd.nextBoolean())));
            } else {
                chatClearer.append(new TextComponent(generateRandomString(68)).withStyle(ChatFormatting.OBFUSCATED));
            }
        }
        player.sendMessage(chatClearer, Util.NIL_UUID);
    }

    private static void giveRespawnEffects(ServerPlayer player) {
        // Give the player blindness and nausea for memoryLoss * memoryLossMultiplier seconds
        int memoryLossMultiplier = SSMPS2Config.SERVER.memoryLossTimeMultiplier.get();
        int memoryLossAmplifier = SSMPS2Config.SERVER.memoryLossAmplifier.get();
        int memoryLoss = SSMPS2Config.SERVER.memoryLossTime.get();

        int duration = (memoryLoss * memoryLossMultiplier) * 20;
        MobEffectInstance blindness = new MobEffectInstance(MobEffects.BLINDNESS, duration, memoryLossAmplifier, false, false, false);
        MobEffectInstance nausea = new MobEffectInstance(MobEffects.CONFUSION, duration, memoryLossAmplifier, false, false, false);
        MobEffectInstance slowness = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, memoryLossAmplifier, false, false, false);
        player.addEffect(blindness);
        player.addEffect(nausea);
        player.addEffect(slowness);
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (SSMPS2Config.SERVER.fallbackServer.get()) {
            return;
        }
        // Ignore if they've respawned due to the end being conquered
        if (!event.isEndConquered()) {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            // Run spawn func
            if (SSMPS2Config.SERVER.enableSpawnpoints.get()) {
                EntityHelpers.teleportServerPlayerToFactionSpawn(player);
            }
            if (player.getPersistentData().contains(SwarmsmpS2.MODID + ":memory_loss_immune")) { return; }
            // Increment their death count
            // Check if they have one already
            int deathCount = player.getPersistentData().getInt(SwarmsmpS2.MODID + ":death_count");
            player.getPersistentData().putInt(SwarmsmpS2.MODID + ":death_count", deathCount + 1);
            LOGGER.info("Player " + player.getDisplayName().getString() + " has respawned");

            // Clear the player's chat
            clearChat(player);

            // Build our message
            int memoryLoss = SSMPS2Config.SERVER.memoryLossTime.get();
            MutableComponent message = new TranslatableComponent(translationKey + "event.death.player", memoryLoss).withStyle(ChatFormatting.RED);

            // Give effects to the player
            giveRespawnEffects(player);

            // Send the message
            player.sendMessage(message, Util.NIL_UUID);

            // Play a scary sound
            player.getLevel().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE, SoundSource.MASTER, 1.0f, 0.2f);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        // Check if the API is even enabled or if we're on the fallback server, if it isn't just return
        if (!SSMPS2Config.SERVER.enableAPI.get() || SSMPS2Config.SERVER.fallbackServer.get()) { return; }


        LOGGER.debug("Starting command check thread");
        exe.execute(() -> {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            SiteAPIHelper apiHelper = new SiteAPIHelper(SSMPS2Config.SERVER.apiKey.get(), SSMPS2Config.SERVER.apiBaseURL.get());
            // Do a check for commands on join
            Commands commandInfo;
            try {
                commandInfo = apiHelper.getCommandInfo(player.getUUID().toString().replace("-", ""));
            } catch (SiteAPIHelper.APIRequestFailedException e) {
                LOGGER.error("Failed to get command info for player " + player.getName().getString() + " with UUID " + player.getUUID() + " with error " + e.getMessage());
                player.connection.disconnect(new TranslatableComponent(translationKey + "connection.error"));
                return;
            }

            if (commandInfo == null) {
                LOGGER.error("Failed to get command info for player " + player.getName().getString() + " with UUID " + player.getUUID() + " with error: commandInfo is null");
                player.connection.disconnect(new TranslatableComponent(translationKey + "connection.error"));
                return;
            }

            // Loop through the commands and execute them
            if (commandInfo.getCommands() != null) {
                for (CommandInfo command : commandInfo.getCommands()) {
                    String parsedCommand = command.getCommand().replace("%player%", player.getName().getString());
                    // Run each command
                    player.getLevel().getServer().getCommands().performCommand(player.getLevel().getServer().createCommandSourceStack(), parsedCommand);
                    try {
                        apiHelper.deleteCommand(command.getId());
                    } catch (SiteAPIHelper.APIRequestFailedException e) {
                        LOGGER.error("Failed to delete command with ID " + command.getId() + " with error " + e.getMessage());
                    }
                }
            }
            // We'll also run 1 command on them, which is to set their fallback server so if this server dies for some reason they'll be redirected to the fallback server
            if (!SSMPS2Config.SERVER.fallbackServer.get()) {
                String fallbackCommand = "fallback " + player.getName().getString() + " fallback.swarmsmp.com";
                player.getLevel().getServer().getCommands().performCommand(player.getLevel().getServer().createCommandSourceStack(), fallbackCommand);
            }
        });
        LOGGER.debug("End of thread");
        // We'll also do a quick little DB thingy here :)
        exe.execute(() -> {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            if (SwarmsmpS2.sqlite == null) { return; }
            SwarmsmpS2.sqlite.createPlayerEvent("join", player.getName().getString());
        });
        if (event.getEntity().getServer() == null) {
            return;
        }
        PlayerList players = event.getEntity().getServer().getPlayerList();
        for (ServerPlayer lPlayer : players.getPlayers()) {
            // Get their persistent storage
            if (lPlayer.getPersistentData().contains(SwarmsmpS2.MODID + ":joinlog")) {
                // Send them a message
                Component message = new TextComponent("[Admin Log] " + event.getPlayer().getName().getString() + " has joined the server.").withStyle(ChatFormatting.RED);
                lPlayer.sendMessage(message, Util.NIL_UUID);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        // We'll also do a quick little DB thingy here :)
        exe.execute(() -> {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            if (SwarmsmpS2.sqlite == null) { return; }
            SwarmsmpS2.sqlite.createPlayerEvent("leave", player.getName().getString());
        });
        if (event.getEntity().getServer() == null) {
            return;
        }
        PlayerList players = event.getEntity().getServer().getPlayerList();
        for (ServerPlayer lPlayer : players.getPlayers()) {
            // Get their persistent storage
            if (lPlayer.getPersistentData().contains(SwarmsmpS2.MODID + ":joinlog")) {
                // Send them a message
                Component message = new TextComponent("[Admin Log] " + event.getPlayer().getName().getString() + " has left the server.").withStyle(ChatFormatting.RED);
                lPlayer.sendMessage(message, Util.NIL_UUID);
            }
        }
    }

    @SubscribeEvent
    public static void noPortals(BlockEvent.PortalSpawnEvent event) {
         event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerKillMob(LivingDeathEvent event) {
        // Make sure the killer is a player
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) { return; }
        String mobName = Objects.requireNonNull(event.getEntity().getType().getRegistryName()).toString();
        exe.execute(() -> {
            if (SwarmsmpS2.sqlite == null) { return; }
            SwarmsmpS2.sqlite.createMobKillEvent(mobName, player.getStringUUID());
        });
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Get their inventory
            List<ItemStack> inventory = player.getInventory().items;
            List<ItemStack> armor = player.getInventory().armor;
            List<ItemStack> offhand = player.getInventory().offhand;
            List<ItemStack> items = new java.util.ArrayList<>(List.copyOf(inventory));
            items.addAll(armor);
            items.addAll(offhand);
            DeathEventObject deathEventObject = new DeathEventObject(player.getStringUUID(), player.blockPosition(), System.currentTimeMillis(), items);
            exe.execute(() -> {
                if (SwarmsmpS2.sqlite == null) { return; }
                SwarmsmpS2.sqlite.createNewPlayerDeath(deathEventObject);
            });
            BlockPos deathPos = player.blockPosition();
            LOGGER.info(player.getName().getString() + " died at " + deathPos.getX() + ", " + deathPos.getY() + ", " + deathPos.getZ() + ".");
            LOGGER.info(event.getSource().getLocalizedDeathMessage(player).getString());
            // Inform any players with the deathlog tag that the player has died
            if (event.getEntity().getServer() == null) {
                return;
            }
            MutableComponent deathReason = (MutableComponent) event.getSource().getLocalizedDeathMessage(player);
            MutableComponent message = new TextComponent("[Admin Log] ");
            message.append(deathReason);
            message.setStyle(Style.EMPTY
                    .applyFormat(ChatFormatting.RED)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableComponent(translationKey + "event.death.hover")))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp @s " + deathPos.getX() + " " + deathPos.getY() + " " + deathPos.getZ()))
            );
            PlayerList players = event.getEntity().getServer().getPlayerList();
            for (ServerPlayer lPlayer : players.getPlayers()) {
                // Get their persistent storage
                if (lPlayer.getPersistentData().contains(SwarmsmpS2.MODID + ":deathlog")) {
                    // Send them a message
                    lPlayer.sendMessage(message, Util.NIL_UUID);
                }
            }
        }
    }

    @SubscribeEvent
    public static void stopBonemealTrees(PlayerInteractEvent.RightClickBlock event) {
        // Check if the player is holding bonemeal in either hand
        ItemStack mainHand = event.getPlayer().getMainHandItem();
        ItemStack offHand = event.getPlayer().getOffhandItem();
        Item boneMeal = Items.BONE_MEAL;
        // If neither hand has bonemeal, return
        if (!mainHand.getItem().equals(boneMeal) && !offHand.getItem().equals(boneMeal)) { return; }
        // Okay so one hand has bonemeal, we'll get the player's team to see if we need to do anything
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        String team = player.getTeam() != null ? player.getTeam().getName() : "none";
        // If the player isn't on the undead or construct just return
        if (!team.equals("undead") && !team.equals("construct")) { return; }
        // If they use bonemeal on moss, cancel the event
        // Get the block they clicked
        BlockState bState = event.getWorld().getBlockState(event.getPos());
        if (bState.getBlock().getRegistryName() == null) { return; }
        String blockName = bState.getBlock().getRegistryName().toString();
        if (blockName.equals("minecraft:moss_block")) {
            event.setCanceled(true);
        }
    }
}
