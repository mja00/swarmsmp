package dev.mja00.swarmsmp.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.mja00.swarmsmp.SSMPS2;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

public class DuelCommand {

    static Logger LOGGER = SSMPS2.LOGGER;

    public static class DuelRequest {
        public final String id;
        public MinecraftServer server;
        public ServerPlayerEntity source;
        public ServerPlayerEntity target;
        public long created;

        public DuelRequest(String s) {
            id = s;
        }
    }

    public static final HashMap<String, DuelRequest> REQUESTS = new HashMap<>();

    public static DuelRequest create(MinecraftServer server, ServerPlayerEntity source, ServerPlayerEntity target) {
        String key;

        do {
            key = String.format("%08X", new Random().nextInt());
        }
        while (REQUESTS.containsKey(key));

        DuelRequest request = new DuelRequest(key);
        request.server = server;
        request.source = source;
        request.target = target;
        request.created = System.currentTimeMillis();
        REQUESTS.put(key, request);
        return request;
    }

    public DuelCommand(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("duel").then(Commands.argument("target", EntityArgument.players()).executes((command) -> {
            return duel(command.getSource(), EntityArgument.getPlayers(command, "target"));
        })).then(Commands.literal("accept").then(Commands.argument("duel_id", StringArgumentType.word()).executes((command) -> {
            return duelAccept(command.getSource(), StringArgumentType.getString(command, "duel_id"));
        }))).then(Commands.literal("decline").then(Commands.argument("duel_id", StringArgumentType.word()).executes((command) -> {
            return duelDecline(command.getSource(), StringArgumentType.getString(command, "duel_id"));
        }))));
    }

    public static int duel(CommandSource source, Collection<ServerPlayerEntity> target) throws CommandSyntaxException {
        ServerPlayerEntity sourcePlayer = source.asPlayer();
        // Make sure target is only one player
        if (target.size() != 1) {
            sourcePlayer.sendMessage(new TranslationTextComponent("commands.duel.invalid_target"), sourcePlayer.getUniqueID());
        }

        ServerPlayerEntity targetPlayer = target.iterator().next();

        // Make sure source and target aren't the same player
        if (sourcePlayer.getUniqueID().equals(targetPlayer.getUniqueID())) {
            sourcePlayer.sendMessage(new TranslationTextComponent("commands.duel.same_player"), sourcePlayer.getUniqueID());
            return 0;
        }

        if (REQUESTS.values().stream().anyMatch(request -> request.source.getUniqueID() == sourcePlayer.getUniqueID() && request.target.getUniqueID() == targetPlayer.getUniqueID())) {
            sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.already_requested", targetPlayer.getDisplayName()).mergeStyle(TextFormatting.RED), sourcePlayer.getUniqueID());
            return 0;
        }

        // Create duel request
        DuelRequest request = create(source.getServer(), sourcePlayer, targetPlayer);

        // Create duel request message components
        StringTextComponent component = new StringTextComponent("Duel Request! [ ");
        component.appendSibling(new StringTextComponent(sourcePlayer.getDisplayName().getString()).mergeStyle(TextFormatting.RED));
        component.appendString(" \u27A1 ");
        component.appendSibling(new StringTextComponent(targetPlayer.getDisplayName().getString()).mergeStyle(TextFormatting.GREEN));
        component.appendString(" ]");

        // Create duel request receive message components
        StringTextComponent component2 = new StringTextComponent("Click one: ");
        component2.appendSibling(new StringTextComponent("Accept \u2714").mergeStyle(Style.EMPTY
                .applyFormatting(TextFormatting.GREEN)
                .applyFormatting(TextFormatting.BOLD)
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + request.id))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to accept duel request")))
        ));

        component2.appendString(" or ");

        component2.appendSibling(new StringTextComponent("Decline \u2716").mergeStyle(Style.EMPTY
                .applyFormatting(TextFormatting.RED)
                .applyFormatting(TextFormatting.BOLD)
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel decline " + request.id))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new StringTextComponent("Click to decline duel request")))
        ));

        // Send duel request messages
        targetPlayer.sendMessage(component, targetPlayer.getUniqueID());
        targetPlayer.sendMessage(component2, targetPlayer.getUniqueID());

        sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.request.sent", targetPlayer.getDisplayName()).mergeStyle(TextFormatting.GREEN), sourcePlayer.getUniqueID());

        return 1;
    }

    public static int duelAccept(CommandSource source, String duel_id) throws CommandSyntaxException {
        DuelRequest request = REQUESTS.get(duel_id);
        ServerPlayerEntity sourcePlayer = source.asPlayer();

        if (request == null) {
          sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.request.not_found").mergeStyle(TextFormatting.RED), sourcePlayer.getUniqueID());
          return 0;
        }

        // Make sure sourcePlayer matches request target
        if (request.target.getUniqueID() != sourcePlayer.getUniqueID()) {
            // Silently return
            return 0;
        }

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerList().getPlayerByUUID(request.source.getUniqueID());
        // Make sure targetPlayer is online
        if (targetPlayer == null) {
            sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.target_not_online").mergeStyle(TextFormatting.RED), sourcePlayer.getUniqueID());
            REQUESTS.remove(request.id);
            return 0;
        }

        // Make sure targetPlayer is not already in a duel
        if (isInDuel(targetPlayer)) {
            sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.target_in_duel").mergeStyle(TextFormatting.RED), sourcePlayer.getUniqueID());
            targetPlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.request.cancelled", sourcePlayer.getDisplayName()).mergeStyle(TextFormatting.RED), targetPlayer.getUniqueID());
            // Remove request
            REQUESTS.remove(request.id);
            return 0;
        }

        // Make sure sourcePlayer is not already in a duel
        if (isInDuel(sourcePlayer)) {
            sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.source_in_duel").mergeStyle(TextFormatting.RED), sourcePlayer.getUniqueID());
            targetPlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.request.cancelled", sourcePlayer.getDisplayName()).mergeStyle(TextFormatting.RED), targetPlayer.getUniqueID());
            // Remove request
            REQUESTS.remove(request.id);
            return 0;
        }

        // Now we're sure this duel can happen
        Boolean success = createDuelBetweenPlayers(sourcePlayer, targetPlayer);

        if (success) {
            // Remove request
            REQUESTS.remove(request.id);
        }

        return 1;
    }

    public static int duelDecline(CommandSource source, String duel_id) throws CommandSyntaxException {
        DuelRequest request = REQUESTS.get(duel_id);
        ServerPlayerEntity sourcePlayer = source.asPlayer();

        if (request == null) {
          sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.request.not_found").mergeStyle(TextFormatting.RED), sourcePlayer.getUniqueID());
          return 0;
        }

        // Make sure sourcePlayer matches request target
        if (request.target.getUniqueID() != sourcePlayer.getUniqueID()) {
            // Silently return
            return 0;
        }

        ServerPlayerEntity targetPlayer = source.getServer().getPlayerList().getPlayerByUUID(request.source.getUniqueID());
        // Make sure targetPlayer is online
        if (targetPlayer == null) {
            sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.target_not_online").mergeStyle(TextFormatting.RED), sourcePlayer.getUniqueID());
            REQUESTS.remove(request.id);
            return 0;
        }

        // Remove request
        REQUESTS.remove(request.id);

        targetPlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.request.declined", sourcePlayer.getDisplayName()).mergeStyle(TextFormatting.RED), targetPlayer.getUniqueID());
        // Confirm it's been declined
        sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.request.declined", targetPlayer.getDisplayName()).mergeStyle(TextFormatting.RED), sourcePlayer.getUniqueID());

        return 1;
    }

    private static boolean isInDuel(ServerPlayerEntity player) {
        return player.getPersistentData().getBoolean(SSMPS2.MOD_ID + ":dueling");
    }

    private static boolean createDuelBetweenPlayers(ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer) {
        CompoundNBT sourcePlayerData = sourcePlayer.getPersistentData();
        CompoundNBT targetPlayerData = targetPlayer.getPersistentData();

        // Set both players dueling tag to true
        sourcePlayerData.putBoolean(SSMPS2.MOD_ID + ":dueling", true);
        targetPlayerData.putBoolean(SSMPS2.MOD_ID + ":dueling", true);

        // Save both players current health
        sourcePlayerData.putFloat(SSMPS2.MOD_ID + ":duel_health", sourcePlayer.getHealth());
        targetPlayerData.putFloat(SSMPS2.MOD_ID + ":duel_health", targetPlayer.getHealth());

        // Save both players current food level
        sourcePlayerData.putInt(SSMPS2.MOD_ID + ":duel_food", sourcePlayer.getFoodStats().getFoodLevel());
        targetPlayerData.putInt(SSMPS2.MOD_ID + ":duel_food", targetPlayer.getFoodStats().getFoodLevel());

        // Save both players current saturation
        sourcePlayerData.putFloat(SSMPS2.MOD_ID + ":duel_saturation", sourcePlayer.getFoodStats().getSaturationLevel());
        targetPlayerData.putFloat(SSMPS2.MOD_ID + ":duel_saturation", targetPlayer.getFoodStats().getSaturationLevel());

        // Save both players duel target
        sourcePlayerData.putUniqueId(SSMPS2.MOD_ID + ":duel_target", targetPlayer.getUniqueID());
        targetPlayerData.putUniqueId(SSMPS2.MOD_ID + ":duel_target", sourcePlayer.getUniqueID());

        // Set both players glowing true
        sourcePlayer.setGlowing(true);
        targetPlayer.setGlowing(true);

        // Inform both players the duel has started
        sourcePlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.started", targetPlayer.getDisplayName()).mergeStyle(TextFormatting.GREEN), sourcePlayer.getUniqueID());
        targetPlayer.sendMessage(new TranslationTextComponent(SSMPS2.translationKey + "commands.duel.started", sourcePlayer.getDisplayName()).mergeStyle(TextFormatting.GREEN), targetPlayer.getUniqueID());

        return true;
    }
}
