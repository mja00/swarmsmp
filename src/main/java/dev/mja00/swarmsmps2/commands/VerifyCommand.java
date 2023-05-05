package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.namelessmc.java_api.NamelessAPI;
import com.namelessmc.java_api.exception.ApiException;
import com.namelessmc.java_api.exception.NamelessException;
import com.namelessmc.java_api.integrations.IntegrationData;
import com.namelessmc.java_api.integrations.MinecraftIntegrationData;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class VerifyCommand {

    static Logger LOGGER = LogManager.getLogger("VERIFY");
    static final String translationKey = SwarmsmpS2.translationKey;

    public VerifyCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("verify").then(Commands.argument("code", StringArgumentType.word())
                .executes((command) -> verify(command.getSource(), StringArgumentType.getString(command, "code")))));
    }

    private int verify(CommandSourceStack source, String code) throws CommandSyntaxException {
        LOGGER.info("Verifying " + source.getTextName() + " with code " + code);
        ServerPlayer player = source.getPlayerOrException();
        // Create a new thread
        Thread thread = new Thread(() -> {
            // Create a new URL object
            try {
                URL url = new URL(SSMPS2Config.SERVER.namelessAPIBaseURL.get());
                final NamelessAPI api = NamelessAPI.builder(url, SSMPS2Config.SERVER.namelessAPIKey.get()).build();
                final IntegrationData integrationData = new MinecraftIntegrationData(player.getUUID(), player.getName().getString());
                api.verifyIntegration(integrationData, code);
                source.sendSuccess(new TranslatableComponent(translationKey + "commands.verify.success").withStyle(ChatFormatting.GREEN), false);
            } catch (ApiException e) {
                switch(e.apiError()) {
                    case CORE_INVALID_CODE -> source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.invalid_code"));
                    case CORE_INTEGRATION_ALREADY_VERIFIED -> source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.already_verified"));
                    case CORE_INTEGRATION_IDENTIFIER_ERROR, CORE_INTEGRATION_USERNAME_ERROR -> source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.error"));
                    default -> source.sendFailure(new TranslatableComponent(translationKey + "commands.verify.api_error"));
                }
                throw new RuntimeException(e);
            } catch (MalformedURLException | NamelessException e) {
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
