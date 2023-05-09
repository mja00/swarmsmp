package dev.mja00.swarmsmps2.events;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.EntityHelpers;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
            EntityHelpers.teleportServerPlayerToFactionSpawn(player);
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
        String endpoint = "whitelist/integration_id:minecraft:" + player.getUUID().toString().replace("-", "");
        String getURL = SSMPS2Config.SERVER.apiBaseURL.get() + endpoint;
        LOGGER.info("Getting whitelist status from API: " + getURL);

        // Get the response
        HttpRequest apiRequest = HttpRequest.newBuilder().GET().uri(URI.create(getURL)).setHeader("User-Agent", "Swarmsmps2").setHeader("Authorization", "Bearer " + SSMPS2Config.SERVER.apiKey.get()).build();
        CompletableFuture<HttpResponse<String>> response = client.sendAsync(apiRequest, HttpResponse.BodyHandlers.ofString());
        String responseBody;

        try {
            responseBody = response.thenApply(HttpResponse::body).get(SSMPS2Config.SERVER.firstTimeout.get(), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            // Check if it's a timeout exception
            if (e instanceof java.util.concurrent.TimeoutException) {
                // Attempt the request again
                LOGGER.error("Error while getting whitelist status from API: Request timed out");
                // Retry it ig lol
                try {
                    CompletableFuture<HttpResponse<String>> response2 = client.sendAsync(apiRequest, HttpResponse.BodyHandlers.ofString());
                    responseBody = response2.thenApply(HttpResponse::body).get(SSMPS2Config.SERVER.secondTimeout.get(), TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException ex) {
                    LOGGER.error("Error while getting whitelist status from API: Request timed out");
                    player.connection.disconnect(new TranslatableComponent(translationKey + "connection.error"));
                    return;
                }
            } else {
                LOGGER.error("Error while getting whitelist status from API", e);
                player.connection.disconnect(new TranslatableComponent(translationKey + "connection.error"));
                return;
            }
        }

        // Check the response code
        if (response.join().statusCode() != 200) {
            LOGGER.error("Error while getting whitelist status from API: " + responseBody);
            player.connection.disconnect(new TranslatableComponent(translationKey + "connection.error"));
            return;
        }

        final Gson gson = new Gson();
        try {
            JoinInfo joinInfo = gson.fromJson(responseBody, JoinInfo.class);
            if (!joinInfo.getAllow()) {
                // Do nothing, they're allowed to join// Disconnect them with the message
                player.connection.disconnect(new TranslatableComponent(translationKey + "connection.disconnected", new TextComponent(joinInfo.getMessage()).withStyle(ChatFormatting.AQUA)));
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
        } catch (JsonSyntaxException | IllegalStateException e) {
            LOGGER.error("Error while parsing whitelist status from API", e);
            player.connection.disconnect(new TranslatableComponent(translationKey + "connection.error"));
        }

    }
}
