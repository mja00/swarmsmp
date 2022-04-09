package dev.mja00.swarmsmps2.commands;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.objects.CharacterInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class BackstoryCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    static final String translationKey = SwarmsmpS2.translationKey;
    static final HttpClient client = HttpClient.newBuilder().build();
    static final HttpRequest apiRequest = HttpRequest.newBuilder().GET().uri(URI.create("https://theairplan.com/test.json")).setHeader("User-Agent", "Swarmsmps2").build();
    static final Gson gson = new Gson();

    public BackstoryCommand(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("backstory").executes(command -> {
            return backstory(command.getSource());
        }));
    }

    private int backstory(CommandSourceStack source) throws CommandSyntaxException {
        // Get the player who executed the command
        ServerPlayer player = source.getPlayerOrException();
        String responseBody;
        MutableComponent message = new TranslatableComponent(translationKey + "commands.backstory.header").withStyle(ChatFormatting.AQUA);
        // Make a HTTP request to the API
        CompletableFuture<HttpResponse<String>> response = client.sendAsync(apiRequest, HttpResponse.BodyHandlers.ofString());

        try {
            responseBody = response.thenApply(HttpResponse::body).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | java.util.concurrent.TimeoutException e) {
            LOGGER.error("Error while getting backstory from API", e);
            source.sendFailure(new TranslatableComponent(translationKey + "commands.backstory.error"));
            return 1;
        }
        if (responseBody.isEmpty()) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.backstory.error"));
            return 1;
        }

        // Parse the response
        CharacterInfo info = gson.fromJson(responseBody, CharacterInfo.class);
        message.append("\n");
        message.append(info.getBackstoryAsComponent().withStyle(ChatFormatting.GOLD));
        message.append("\n");
        message.append(new TranslatableComponent(translationKey + "commands.backstory.footer").withStyle(ChatFormatting.AQUA));
        source.sendSuccess(message, false);
        return 0;
    }
}
