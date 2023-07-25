package dev.mja00.swarmsmps2.commands;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.objects.VerifyInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class VerifyCommand {

    static Logger LOGGER = LogManager.getLogger("VERIFY");
    static final String translationKey = SwarmsmpS2.translationKey;
    static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();

    public VerifyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("verify").then(Commands.argument("code", StringArgumentType.word())
                .executes((command) -> verify(command.getSource(), StringArgumentType.getString(command, "code")))));
    }

    private int verify(CommandSourceStack source, String code) throws CommandSyntaxException {
        LOGGER.info("Verifying " + source.getTextName() + " with code " + code);
        ServerPlayer player = source.getPlayerOrException();
        // Create a new thread
        Thread thread = new Thread(() -> {
            URI uri = URI.create(SSMPS2Config.SERVER.apiBaseURL.get() + "integration/verify");
            // Create our request body as a JSON string
            try {
                Gson gson = new Gson();
                VerifyInfo verifyInfo = new VerifyInfo(player.getName().getString(), player.getUUID().toString().replace("-", ""), code);
                HttpRequest postRequest = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(gson.toJson(verifyInfo))).uri(uri)
                        .setHeader("Authorization", "Bearer " + SSMPS2Config.SERVER.apiKey.get())
                        .setHeader("Content-Type", "application/json").build();
                HttpResponse<String> response = client.send(postRequest, HttpResponse.BodyHandlers.ofString());
                // Get the response code
                int responseCode = response.statusCode();
                switch (responseCode) {
                    case 200 -> {
                        // Validation was a success
                        //LOGGER.info("Success");
                        source.sendSuccess(new TranslatableComponent(translationKey + "commands.verify.success").withStyle(ChatFormatting.GREEN), false);
                    }
                    case 400 -> {
                        // Bad request, get the "error" field from the response
                        String error = response.body().split("\"error\":\"")[1].split("\"")[0];
                        // Switch on the error
                        switch (error) {
                            case "core:integration_identifier_errors", "core:integration_username_errors" ->
                                    source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.error"));
                            case "core:invalid_code" ->
                                    source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.invalid_code"));
                            case "core:integration_already_verified" ->
                                    source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.already_verified"));
                            default ->
                                    source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.api_error"));
                        }
                        LOGGER.error(response.body());
                        throw new RuntimeException(error);
                    }
                    default -> {
                        // Idk some unknown issue
                        source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.api_error"));
                        // Print out the response body
                        LOGGER.error(response.body());
                        throw new RuntimeException("Unknown error");
                    }
                }
            } catch (IOException | InterruptedException e) {
                source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.api_error"));
                throw new RuntimeException(e);
            }
        });
        try {
            thread.start();
        } catch (RuntimeException e) {
            return 0;
        }
        return 1;
    }
}
