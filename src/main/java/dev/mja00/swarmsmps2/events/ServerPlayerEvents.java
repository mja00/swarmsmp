package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.EntityHelpers;
import dev.mja00.swarmsmps2.helpers.SiteAPIHelper;
import dev.mja00.swarmsmps2.objects.CommandInfo;
import dev.mja00.swarmsmps2.objects.Commands;
import dev.mja00.swarmsmps2.objects.JoinInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID, value = Dist.DEDICATED_SERVER)
public class ServerPlayerEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final String translationKey = SwarmsmpS2.translationKey;
    static final Random rnd = new Random();
    static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();


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
        // Check if the API is even enabled, if it isn't just return
        if (!SSMPS2Config.SERVER.enableAPI.get()) { return; }
        // Get player
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        // Check if their UUID is in the bypassed player list
        if (SSMPS2Config.SERVER.bypassedPlayers.get().contains(player.getUUID().toString())) {
            player.sendMessage(new TranslatableComponent(translationKey + "connection.bypassed").withStyle(ChatFormatting.AQUA), Util.NIL_UUID);
            return;
        }
        SiteAPIHelper apiHelper = new SiteAPIHelper(SSMPS2Config.SERVER.apiKey.get(), SSMPS2Config.SERVER.apiBaseURL.get());
        JoinInfo joinInfo;
        try {
            joinInfo = apiHelper.getJoinInfo(player.getUUID().toString().replace("-", ""));
        } catch (SiteAPIHelper.APIRequestFailedException e) {
            LOGGER.error("Failed to get join info for player " + player.getName().getString() + " with UUID " + player.getUUID() + " with error " + e.getMessage());
            player.connection.disconnect(new TranslatableComponent(translationKey + "connection.error"));
            return;
        }
        if (!joinInfo.getAllow()) {
            // Here we also want to do a fallback server check, as we really only care about if they're whitelisted or not, so we'll do some error checking
            if (SSMPS2Config.SERVER.fallbackServer.get()) {
                String errorMsg = joinInfo.getMessage();
                // If it's either "You are not whitelisted." or "You are banned from the server for: " then we want to block their connection, otherwise let them in
                if (Objects.equals(errorMsg, "You are not whitelisted.") || errorMsg.startsWith("You are banned from the server for: ")) {
                    player.connection.disconnect(new TranslatableComponent(translationKey + "connection.disconnected", new TextComponent(errorMsg).withStyle(ChatFormatting.AQUA)));
                }
                return;
            }
            // Do nothing, they're allowed to join// Disconnect them with the message
            player.connection.disconnect(new TranslatableComponent(translationKey + "connection.disconnected", new TextComponent(joinInfo.getMessage()).withStyle(ChatFormatting.AQUA)));
            return;
        }
        // If their message is "Bypass" then send a message saying they bypassed the whitelist checks
        if (Objects.equals(joinInfo.getMessage(), "Bypass")) {
            player.sendMessage(new TranslatableComponent(translationKey + "connection.bypassed").withStyle(ChatFormatting.AQUA), Util.NIL_UUID);
        }
        // Check if it's MC-Verify and send a message
        if (Objects.equals(joinInfo.getMessage(), "MC-Verify")) {
            player.sendMessage(new TranslatableComponent(translationKey + "connection.mcverify").withStyle(ChatFormatting.AQUA), Util.NIL_UUID);
            // Compile a message that the user can clicky to open a link to the forum post explaining how to do it
            MutableComponent message = new TextComponent("Click here to learn how to verify your Minecraft account.")
                    .setStyle(Style.EMPTY
                            .applyFormat(ChatFormatting.BLUE)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, SSMPS2Config.SERVER.verificationFAQURL.get()))
                    );
            player.sendMessage(message, Util.NIL_UUID);

        }
        // Now we check to see if we're the fallback server and return early
        // This check is probably redundant, but we REALLY don't want to run these commands on the fallback server
        if (SSMPS2Config.SERVER.fallbackServer.get()) {
            return;
        }

        // Do a check for commands on join
        Commands commandInfo;
        try {
            commandInfo = apiHelper.getCommandInfo(player.getUUID().toString().replace("-", ""));
        } catch (SiteAPIHelper.APIRequestFailedException e) {
            LOGGER.error("Failed to get command info for player " + player.getName().getString() + " with UUID " + player.getUUID() + " with error " + e.getMessage());
            player.connection.disconnect(new TranslatableComponent(translationKey + "connection.error"));
            return;
        }

        // Loop through the commands and execute them
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

    @SubscribeEvent
    public static void noPortals(BlockEvent.PortalSpawnEvent event) {
         event.setCanceled(true);
    }
}
