package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.DuelHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;

public class DuelCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    private static final String translationKey = SwarmsmpS2.translationKey;
    private static final String MOD_ID = SwarmsmpS2.MODID;

    public static class DuelRequest {
        public final String id;
        public MinecraftServer server;
        public ServerPlayer source;
        public ServerPlayer target;
        public long created;

        public DuelRequest(String s) { id = s; }
    }

    public static final HashMap<String, DuelRequest> REQUESTS = new HashMap<>();

    public static DuelRequest create(MinecraftServer server, ServerPlayer source, ServerPlayer target) {
        String key;
        do {
            key = String.format("%08X", new Random().nextInt());
        } while (REQUESTS.containsKey(key));

        DuelRequest request = new DuelRequest(key);
        request.server = server;
        request.source = source;
        request.target = target;
        request.created = System.currentTimeMillis();
        REQUESTS.put(key, request);
        return request;
    }

    public DuelCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("duel").then(Commands.argument("target", EntityArgument.players()).executes((command) -> {
            return duel(command.getSource(), EntityArgument.getPlayers(command, "target"));
        })).then(Commands.literal("accept").then(Commands.argument("duel_id", StringArgumentType.word()).executes((command) -> {
            return duelAccept(command.getSource(), StringArgumentType.getString(command, "duel_id"));
        }))).then(Commands.literal("decline").then(Commands.argument("duel_id", StringArgumentType.word()).executes((command) -> {
            return duelDecline(command.getSource(), StringArgumentType.getString(command, "duel_id"));
        }))).then(Commands.literal("forfeit").executes((command) -> {
           return duelForfeit(command.getSource());
        })));
    }

    private int duel(CommandSourceStack source, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = source.getPlayerOrException();

        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.duel.invalid_target"));
            return 0;
        }

        ServerPlayer targetPlayer = targets.iterator().next();

        // Make sure they're not the same player
        if (sourcePlayer.getUUID().equals(targetPlayer.getUUID())) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.duel.same_player"));
            return 0;
        }

        // Make sure they haven't already requested a duel
        if (REQUESTS.values().stream().anyMatch(request -> request.source.getUUID() == sourcePlayer.getUUID() && request.target.getUUID() == targetPlayer.getUUID())) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.duel.already_requested", targetPlayer.getDisplayName()));
            return 0;
        }

        // Create duel request
        DuelRequest request = create(source.getServer(), sourcePlayer, targetPlayer);

        // Create duel reqeust message components
        TextComponent component = new TextComponent("Duel Request! [ ");
        component.append(new TextComponent(sourcePlayer.getDisplayName().getString()).withStyle(ChatFormatting.RED));
        component.append(new TextComponent(" \u27A1 "));
        component.append(new TextComponent(targetPlayer.getDisplayName().getString()).withStyle(ChatFormatting.GREEN));
        component.append(new TextComponent(" ]"));

        // Create duel request receive message components
        TextComponent component2 = new TextComponent("Click one: ");
        component2.append(new TextComponent("Accept \u2714")
                .setStyle(Style.EMPTY
                        .applyFormat(ChatFormatting.GREEN)
                        .applyFormat(ChatFormatting.BOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + request.id))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to accept duel request")))
                )
        );;

        component2.append(" or ");

        component2.append(new TextComponent("Decline \u2716")
                .setStyle(Style.EMPTY
                        .applyFormat(ChatFormatting.RED)
                        .applyFormat(ChatFormatting.BOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel decline " + request.id))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to decline duel request")))
                )
        );

        // Send duel request message
        targetPlayer.sendMessage(component, sourcePlayer.getUUID());
        targetPlayer.sendMessage(component2, sourcePlayer.getUUID());

        return 1;
    }

    private int duelDecline(CommandSourceStack source, String duelId) throws CommandSyntaxException {
        DuelRequest request = REQUESTS.get(duelId);
        ServerPlayer sourcePlayer = source.getPlayerOrException();

        if (request == null) {
            sourcePlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.request.not_found").withStyle(ChatFormatting.RED), DUMMY);
            return 0;
        }

        // Make sure the source player is the target player
        if (request.target.getUUID() != sourcePlayer.getUUID()) {
            // Silently fail
            return 0;
        }

        ServerPlayer targetPlayer = request.source;
        if (targetPlayer == null) {
            sourcePlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.target_not_online").withStyle(ChatFormatting.RED), DUMMY);
            REQUESTS.remove(request.id);
            return 0;
        }

        // Remove request
        REQUESTS.remove(request.id);

        targetPlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.request.declined", sourcePlayer.getDisplayName()).withStyle(ChatFormatting.RED), DUMMY);
        // Confirm it's been declined
        sourcePlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.request.declined", targetPlayer.getDisplayName()).withStyle(ChatFormatting.RED), DUMMY);

        return 1;
    }

    private int duelAccept(CommandSourceStack source, String duelId) throws CommandSyntaxException {
        DuelRequest request = REQUESTS.get(duelId);
        ServerPlayer sourcePlayer = source.getPlayerOrException();

        if (request == null) {
            sourcePlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.request.not_found").withStyle(ChatFormatting.RED), DUMMY);
            return 0;
        }

        // Make sure sourcePlayer matches request target
        if (request.target.getUUID() != sourcePlayer.getUUID()) {
            // Silently return
            return 0;
        }

        ServerPlayer targetPlayer = request.source;
        // Make sure targetPlayer is online
        if (targetPlayer == null) {
            sourcePlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.target_not_online").withStyle(ChatFormatting.RED), DUMMY);
            REQUESTS.remove(request.id);
            return 0;
        }

        // Make sure targetPlayer is not already in a duel
        if (isInDuel(targetPlayer)) {
            sourcePlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.target_in_duel").withStyle(ChatFormatting.RED), DUMMY);
            targetPlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.request.cancelled", sourcePlayer.getDisplayName()).withStyle(ChatFormatting.RED), DUMMY);
            // Remove request
            REQUESTS.remove(request.id);
            return 0;
        }

        // Make sure sourcePlayer is not already in a duel
        if (isInDuel(sourcePlayer)) {
            sourcePlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.source_in_duel").withStyle(ChatFormatting.RED), DUMMY);
            targetPlayer.sendMessage(new TranslatableComponent(translationKey + "commands.duel.request.cancelled", sourcePlayer.getDisplayName()).withStyle(ChatFormatting.RED), DUMMY);
            // Remove request
            REQUESTS.remove(request.id);
            return 0;
        }

        // Now we're sure this duel can happen
        boolean success = DuelHelper.createDuelBetweenPlayers(sourcePlayer, targetPlayer, false);

        if (success) {
            // Remove request
            REQUESTS.remove(request.id);
        }

        return 1;
    }

    private int duelForfeit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!isInDuel(player)) {
            player.sendMessage(new TranslatableComponent(translationKey + "commands.duel.not_in_duel").withStyle(ChatFormatting.RED), DUMMY);
            return 0;
        }

        UUID duelOpponentUUID = player.getPersistentData().getUUID(MOD_ID + ":duel_target");
        ServerPlayer duelOpponent = source.getServer().getPlayerList().getPlayer(duelOpponentUUID);

        DuelHelper.endDuelBetweenPlayers(player, duelOpponent);

        return 1;
    }

    private boolean isInDuel(ServerPlayer player) {
        CompoundTag playerData = player.getPersistentData();
        if (playerData.contains(MOD_ID + ":dueling")) {
            return playerData.getBoolean(MOD_ID + ":dueling");
        } else {
            return false;
        }
    }
}
