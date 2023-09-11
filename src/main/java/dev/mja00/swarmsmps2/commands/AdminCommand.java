package dev.mja00.swarmsmps2.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.DuelHelper;
import dev.mja00.swarmsmps2.helpers.EntityHelpers;
import dev.mja00.swarmsmps2.network.SwarmSMPPacketHandler;
import dev.mja00.swarmsmps2.network.packets.SaoModePacket;
import dev.mja00.swarmsmps2.objects.BlockEventObject;
import dev.mja00.swarmsmps2.objects.DeathEventObject;
import dev.mja00.swarmsmps2.objects.MobKillObject;
import dev.mja00.swarmsmps2.utility.DeadPlayerInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.EntitySummonArgument;
import net.minecraft.commands.arguments.ItemEnchantmentArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.mja00.swarmsmps2.SSMPS2Config.getSpawnpointForFaction;

public class AdminCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    static final String translationKey = SwarmsmpS2.translationKey;
    static final List<String> FACTIONS = Arrays.asList("swarm", "construct", "undead", "natureborn", "default", "debug1" , "debug2", "debug3", "debug4", "debug5");

    // Our custom suggestions provider for faction names
    public static final SuggestionProvider<CommandSourceStack> FACTION_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(FACTIONS.stream(), builder);
    };

    public static class AdminMessage {
        public final String id;
        public  MinecraftServer server;
        public ServerPlayer source;
        public ServerPlayer target;
        public long created;

        public AdminMessage(String s) { id = s; }
    }

    public static final HashMap<String, AdminMessage> MESSAGES = new HashMap<>();

    public static AdminMessage createAdminMessage(MinecraftServer server, ServerPlayer source, ServerPlayer target) {
        String key;
        do {
            key = String.format("%08X", new Random().nextInt());
        } while (MESSAGES.containsKey(key));

        AdminMessage adminMessage = new AdminMessage(key);
        adminMessage.server = server;
        adminMessage.source = source;
        adminMessage.target = target;
        adminMessage.created = System.currentTimeMillis();
        MESSAGES.put(key, adminMessage);
        return adminMessage;
    }

    public AdminCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("admin")
                .requires((command) -> command.hasPermission(2))
                .then(Commands.literal("sao").then(Commands.argument("state", BoolArgumentType.bool())
                        .executes((command) -> triggerSAOMode(command.getSource(), BoolArgumentType.getBool(command, "state")))
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> triggerSAOMode(command.getSource(), BoolArgumentType.getBool(command, "state"), EntityArgument.getPlayers(command, "player"))))))
                .then(Commands.literal("set_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word())
                        .executes((command) -> setTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"))))))
                .then(Commands.literal("remove_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word())
                        .executes((command) -> removeTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"))))))
                .then(Commands.literal("check_tag").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("tag", StringArgumentType.word())
                        .executes((command) -> checkTag(command.getSource(), EntityArgument.getPlayers(command, "target"), StringArgumentType.getString(command, "tag"))))))
                .then(Commands.literal("start_duel").then(Commands.argument("first_player", EntityArgument.players()).then(Commands.argument("second_player", EntityArgument.players())
                        .executes((command) -> startDuel(command.getSource(), EntityArgument.getPlayers(command, "first_player"), EntityArgument.getPlayers(command, "second_player"))))))
                .then(Commands.literal("get_head").then(Commands.argument("target", EntityArgument.players())
                        .executes((command) -> giveHeadOfPlayer(command.getSource(), EntityArgument.getPlayers(command, "target")))))
                .then(Commands.literal("give_head").then(Commands.argument("target", EntityArgument.players()).then(Commands.argument("head_target", EntityArgument.players())
                        .executes(command -> giveHeadOfPlayerToPlayer(command.getSource(), EntityArgument.getPlayers(command, "target"), EntityArgument.getPlayers(command, "head_target"))))))
                .then(Commands.literal("end_duel").then(Commands.argument("player", EntityArgument.players())
                        .executes((command) -> endDuel(command.getSource(), EntityArgument.getPlayers(command, "player")))))
                .then(Commands.literal("deaths")
                        .then(Commands.literal("get").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> getPlayerDeathCount(command.getSource(), EntityArgument.getPlayers(command, "player")))))
                        .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.players()).then(Commands.argument("count", IntegerArgumentType.integer())
                                .executes((command) -> setPlayerDeathCount(command.getSource(), EntityArgument.getPlayers(command, "player"), IntegerArgumentType.getInteger(command, "count"))))))
                        .then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> resetPlayerDeathCount(command.getSource(), EntityArgument.getPlayers(command, "player"))))))
                .then(Commands.literal("factions")
                        .then(Commands.literal("spawn").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTION_SUGGESTIONS)
                                .executes((command) -> teleportToFactionSpawn(command.getSource(), StringArgumentType.getString(command, "faction")))
                                .then(Commands.argument("player", EntityArgument.players())
                                    .executes((command) -> teleportPlayersToFactionSpawn(command.getSource(), StringArgumentType.getString(command, "faction"), EntityArgument.getPlayers(command, "player")))))))
                .then(Commands.literal("items")
                        .then(Commands.literal("get_tags")
                                .executes((command) -> getTagsForItem(command.getSource()))))
                .then(Commands.literal("log")
                        .then(Commands.literal("mob").then(Commands.argument("mob", EntitySummonArgument.id()).suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                                .executes((command) -> logMob(command.getSource(), EntitySummonArgument.getSummonableEntity(command, "mob"), 10, 0))))
                        .then(Commands.literal("block").then(Commands.argument("location", Vec3Argument.vec3())
                                    .executes((command) -> logBlock(command.getSource(), Vec3Argument.getCoordinates(command, "location"), 0, 10, 0))
                                .then(Commands.argument("scale", IntegerArgumentType.integer(0, 10))
                                    .executes((command) -> logBlock(command.getSource(), Vec3Argument.getCoordinates(command, "location"), IntegerArgumentType.getInteger(command, "scale"), 10, 0))
                                    .then(Commands.argument("limit", IntegerArgumentType.integer(1, 20))
                                        .executes((command) -> logBlock(command.getSource(), Vec3Argument.getCoordinates(command, "location"), IntegerArgumentType.getInteger(command, "scale"), IntegerArgumentType.getInteger(command, "limit"), 0))
                                        .then(Commands.argument("offset", IntegerArgumentType.integer())
                                            .executes((command) -> logBlock(command.getSource(), Vec3Argument.getCoordinates(command, "location"), IntegerArgumentType.getInteger(command, "scale"), IntegerArgumentType.getInteger(command, "limit"), IntegerArgumentType.getInteger(command, "offset"))))))))
                        .then(Commands.literal("death").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> logDeath(command.getSource(), EntityArgument.getPlayers(command, "player"), 10, 0))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 20))
                                        .executes((command) -> logDeath(command.getSource(), EntityArgument.getPlayers(command, "player"), IntegerArgumentType.getInteger(command, "limit"), 0))
                                        .then(Commands.argument("offset", IntegerArgumentType.integer())
                                                .executes((command) -> logDeath(command.getSource(), EntityArgument.getPlayers(command, "player"), IntegerArgumentType.getInteger(command, "limit"), IntegerArgumentType.getInteger(command, "offset")))
                                                .then(Commands.argument("id", IntegerArgumentType.integer())
                                                        .executes((command) -> getSingleDeath(command.getSource(), EntityArgument.getPlayers(command, "player"), IntegerArgumentType.getInteger(command, "id"))))))))
                        .then(Commands.literal("player").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> logPlayer(command.getSource(), EntityArgument.getPlayers(command, "player"), 10, 0))
                                .then(Commands.argument("limit", IntegerArgumentType.integer(1, 20))
                                    .executes((command) -> logPlayer(command.getSource(), EntityArgument.getPlayers(command, "player"), IntegerArgumentType.getInteger(command, "limit"), 0))
                                .then(Commands.argument("offset", IntegerArgumentType.integer())
                                    .executes((command) -> logPlayer(command.getSource(), EntityArgument.getPlayers(command, "player"), IntegerArgumentType.getInteger(command, "limit"), IntegerArgumentType.getInteger(command, "offset"))))))))
                .then(Commands.literal("self")
                        .then(Commands.literal("coords")
                                .executes((command) -> getCoords(command.getSource())))
                        .then(Commands.literal("biome")
                                .executes((command) -> getBiome(command.getSource()))))
                .then(Commands.literal("enchant").then(Commands.argument("player", EntityArgument.players()).then(Commands.argument("enchantment", ItemEnchantmentArgument.enchantment())
                                .executes((command) -> enchantPlayerItem(command.getSource(), EntityArgument.getPlayers(command, "player"), ItemEnchantmentArgument.getEnchantment(command, "enchantment"), 1))
                                .then(Commands.argument("level", IntegerArgumentType.integer())
                                        .executes((command) -> enchantPlayerItem(command.getSource(), EntityArgument.getPlayers(command, "player"), ItemEnchantmentArgument.getEnchantment(command, "enchantment"), IntegerArgumentType.getInteger(command, "level")))))))
                .then(Commands.literal("message")
                        .then(Commands.argument("player", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message())
                                .executes((command) -> sendMessageToPlayer(command.getSource(), EntityArgument.getPlayers(command, "player"), MessageArgument.getMessage(command, "message"))))))
                .then(Commands.literal("players")
                        .then(Commands.literal("get_effects").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> getPlayerEffects(command.getSource(), EntityArgument.getPlayers(command, "player")))))
                        .then(Commands.literal("get_team").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> getPlayerTeam(command.getSource(), EntityArgument.getPlayers(command, "player")))))
                        .then(Commands.literal("get_health").then(Commands.argument("player", EntityArgument.players())
                                .executes((command) -> getPlayerHealth(command.getSource(), EntityArgument.getPlayers(command, "player")))))
                        .then(Commands.literal("set_health").then(Commands.argument("player", EntityArgument.players()).then(Commands.argument("health", IntegerArgumentType.integer())
                                .executes((command) -> setPlayerHealth(command.getSource(), EntityArgument.getPlayers(command, "player"), IntegerArgumentType.getInteger(command, "health")))))))
                .then(Commands.literal("config")
                        .then(Commands.literal("edit")
                                .then(Commands.literal("bypass")
                                        .then(Commands.literal("add").then(Commands.argument("uuid", StringArgumentType.word())
                                                .executes((command) -> addUUIDToBypass(command.getSource(), StringArgumentType.getString(command, "uuid")))))
                                        .then(Commands.literal("remove").then(Commands.argument("uuid", StringArgumentType.word())
                                                .executes((command) -> removeUUIDFromBypass(command.getSource(), StringArgumentType.getString(command, "uuid"))))))
                                .then(Commands.literal("spawnpoint")
                                        .then(Commands.literal("set").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTION_SUGGESTIONS).then(Commands.argument("location", Vec3Argument.vec3())
                                                .executes((command) -> setFactionSpawnpoint(command.getSource(), StringArgumentType.getString(command, "faction"), Vec3Argument.getCoordinates(command, "location"))))))
                                        .then(Commands.literal("get").then(Commands.argument("faction", StringArgumentType.word()).suggests(FACTION_SUGGESTIONS)
                                                .executes((command) -> getFactionSpawnpoint(command.getSource(), StringArgumentType.getString(command, "faction")))))))
                        .then(Commands.literal("reload")
                                .executes((command) -> reloadConfigFile(command.getSource())))));
    }

    private int enchantPlayerItem(CommandSourceStack source, Collection<ServerPlayer> targets, Enchantment pEnchantment, int level) {
        for (ServerPlayer target : targets) {
            ItemStack itemStack = target.getMainHandItem();
            itemStack.enchant(pEnchantment, level);
        }
        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.enchant.success.single", targets.iterator().next().getDisplayName(), pEnchantment.getFullname(level)), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.enchant.success.multiple", targets.size(), pEnchantment.getFullname(level)), true);
        }
        return 1;
    }

    private int logPlayer(CommandSourceStack source, Collection<ServerPlayer> targets, int limit, int offset) {
        if (offset < 0) {
            offset = 0;
        }
        // We want to get the block logs of the player
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.players.too_many"));
            return 0;
        }
        // Get the player
        ServerPlayer player = targets.iterator().next();
        // Get their UUID
        String playerUUID = player.getStringUUID();
        String playerName = player.getName().getString();
        BlockEventObject[] blockEvents = SwarmsmpS2.sqlite.getPlayerWorldEvents(playerUUID, limit, offset);
        boolean hitEndOfLogs = false;
        MutableComponent component = new TextComponent("---------------\n").withStyle(ChatFormatting.GOLD);
        // Iterate the logs
        for (BlockEventObject blockEvent : blockEvents) {
            // If we hit a null one just end the for loop as we've reached the end of the logs
            if (blockEvent == null) {
                hitEndOfLogs = true;
                break;
            }
            MutableComponent message = new TranslatableComponent(translationKey + "commands.admin.block_logs.get", playerName, blockEvent.getEventPretty(), blockEvent.getActualBlockName(), blockEvent.getX(), blockEvent.getY(), blockEvent.getZ(), blockEvent.humanizeTimestamp())
                    .setStyle(Style.EMPTY
                            .applyFormat(blockEvent.getEventColor())
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to teleport to this location")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + blockEvent.getX() + " " + blockEvent.getY() + " " + blockEvent.getZ()))
                    );
            component.append(message);
        }
        // We'll create little buttons down here to travel between pages
        MutableComponent previousButton = new TextComponent("<< ")
                .setStyle(Style.EMPTY
                        .applyFormat(offset == 0 ? ChatFormatting.GRAY : ChatFormatting.RED)
                        .applyFormat(ChatFormatting.BOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log player " + playerName + " " + limit + " " + (offset - limit)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Previous Page")))
                );
        MutableComponent nextButton = new TextComponent(" >>")
                .setStyle(Style.EMPTY
                        .applyFormat(hitEndOfLogs ? ChatFormatting.GRAY : ChatFormatting.GREEN)
                        .applyFormat(ChatFormatting.BOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log player " + playerName + " " + limit + " " + (offset + limit)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Next Page")))
                );
        component.append(previousButton);
        component.append("-----");
        component.append(nextButton);
        source.sendSuccess(component, false);
        return 1;
    }

    private int logMob(CommandSourceStack source, ResourceLocation mob, int limit, int offset) {
        if (offset < 0) {
            offset = 0;
        }
        MobKillObject[] mobKills = SwarmsmpS2.sqlite.getPlayersFromMob(mob.toString(), limit, offset);
        boolean hitEndOfLogs = false;
        MutableComponent component = new TextComponent("---------------\n").withStyle(ChatFormatting.GOLD);
        // Iterate the logs
        for (MobKillObject killObject : mobKills) {
            // If we hit a null one just end the for loop as we've reached the end of the logs
            if (killObject == null) {
                hitEndOfLogs = true;
                break;
            }
            GameProfileCache profileCache = source.getServer().getProfileCache();
            Optional<GameProfile> profile = profileCache.get(killObject.getPlayerUUID());
            String playerName = profile.isPresent() ? profile.get().getName() : "Unknown";
            MutableComponent message = new TranslatableComponent(translationKey + "commands.admin.mob_logs.get", playerName, killObject.getActualMobName(), killObject.humanizeTimestamp()).withStyle(ChatFormatting.AQUA);
            component.append(message);
        }
        // We'll create little buttons down here to travel between pages
        MutableComponent previousButton = new TextComponent("<< ")
                .setStyle(Style.EMPTY
                        .applyFormat(offset == 0 ? ChatFormatting.GRAY : ChatFormatting.RED)
                        .applyFormat(ChatFormatting.BOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log mob " + mob.getPath() + " " + limit + " " + (offset - limit)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Previous Page")))
                );
        MutableComponent nextButton = new TextComponent(" >>")
                .setStyle(Style.EMPTY
                        .applyFormat(hitEndOfLogs ? ChatFormatting.GRAY : ChatFormatting.GREEN)
                        .applyFormat(ChatFormatting.BOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log mob " + mob.getPath() + " " + limit + " " + (offset + limit)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Next Page")))
                );
        component.append(previousButton);
        component.append("-----");
        component.append(nextButton);
        source.sendSuccess(component, false);
        return 1;
    }

    private int logBlock(CommandSourceStack source, Coordinates pPosition, int scale, int limit, int offset) {
        // We want to do a scan around the position given to us, our easiest is if we have no scale, then it's just the position
        // If we have a scale, we'll want to scan around the position
        Vec3 vec3 = pPosition.getPosition(source);
        BlockPos bPos = new BlockPos(vec3);
        BlockEventObject[] blockEvents;
        if (scale == 0) {
            blockEvents = SwarmsmpS2.sqlite.getWorldEventsAtBlock(bPos, limit, offset);
        } else {
            // We've got a scale so we gotta work with that
            blockEvents = SwarmsmpS2.sqlite.getWorldEventsAtBlocks(getBlockPositionsAround(bPos, scale), limit, offset);
        }
        boolean hitEndOfLogs = false;
        MutableComponent component = new TextComponent("---------------\n").withStyle(ChatFormatting.GOLD);
        // Iterate the logs
        for (BlockEventObject blockEvent : blockEvents) {
            // If we hit a null one just end the for loop as we've reached the end of the logs
            if (blockEvent == null) {
                hitEndOfLogs = true;
                break;
            }
            GameProfileCache profileCache = source.getServer().getProfileCache();
            Optional<GameProfile> profile = profileCache.get(blockEvent.getPlayerUUID());
            String playerName = profile.isPresent() ? profile.get().getName() : "Unknown";
            MutableComponent message = new TranslatableComponent(translationKey + "commands.admin.block_logs.get", playerName, blockEvent.getEventPretty(), blockEvent.getActualBlockName(), blockEvent.getX(), blockEvent.getY(), blockEvent.getZ(), blockEvent.humanizeTimestamp())
                    .setStyle(Style.EMPTY
                            .applyFormat(blockEvent.getEventColor())
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to teleport to this player")))
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + playerName))
                    );
            component.append(message);
        }
        // We'll create little buttons down here to travel between pages
        MutableComponent previousButton = new TextComponent("<< ")
                .setStyle(Style.EMPTY
                        .applyFormat(offset == 0 ? ChatFormatting.GRAY : ChatFormatting.RED)
                        .applyFormat(ChatFormatting.BOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log block " + bPos.getX() + " " + bPos.getY() + " " + bPos.getZ() + " " + scale + " " + limit + " " + (offset - limit)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Previous Page")))
                );

        MutableComponent nextButton = new TextComponent(" >>")
                .setStyle(Style.EMPTY
                        .applyFormat(hitEndOfLogs ? ChatFormatting.GRAY : ChatFormatting.GREEN)
                        .applyFormat(ChatFormatting.BOLD)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log block " + bPos.getX() + " " + bPos.getY() + " " + bPos.getZ() + " " + scale + " " + limit + " " + (offset + limit)))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Next Page")))
                );
        component.append(previousButton);
        component.append("-----");
        component.append(nextButton);
        source.sendSuccess(component, false);
        return 1;
    }

    private int logDeath(CommandSourceStack source, Collection<ServerPlayer> targets, int limit, int offset) {
        if (offset < 0) {
            offset = 0;
        }
        // We want to get the block logs of the player
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.players.too_many"));
            return 0;
        }
        try {
            // Get the player
            ServerPlayer player = targets.iterator().next();
            // Get their UUID
            String playerUUID = player.getStringUUID();
            String playerName = player.getName().getString();
            DeathEventObject[] deathEventObjects = SwarmsmpS2.sqlite.getPlayerDeaths(playerUUID, limit, offset);
            boolean hitEndOfLogs = false;
            MutableComponent component = new TextComponent("---------------\n").withStyle(ChatFormatting.GOLD);
            // Iterate the logs
            for (DeathEventObject deathEventObject : deathEventObjects) {
                // If we hit a null one just end the for loop as we've reached the end of the logs
                if (deathEventObject == null) {
                    hitEndOfLogs = true;
                    break;
                }
                if (deathEventObject.getItems().isEmpty()) {
                    // Something went wrong parsing the JSON upstream, we'll send failure here
                    source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.death_logs.no_items", playerName));
                    return 0;
                }
                BlockPos deathPos = deathEventObject.getPos();
                MutableComponent message = new TranslatableComponent(translationKey + "commands.admin.death_logs.get", playerName, deathPos.getX(), deathPos.getY(), deathPos.getZ(), deathEventObject.humanizeTimestamp())
                        .setStyle(Style.EMPTY
                                .applyFormat(ChatFormatting.RED)
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to view inventory")))
                                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log death " + playerName + " 1 1 " + deathEventObject.getId() ))
                        );
                component.append(message);
            }
            // We'll create little buttons down here to travel between pages
            MutableComponent previousButton = new TextComponent("<< ")
                    .setStyle(Style.EMPTY
                            .applyFormat(offset == 0 ? ChatFormatting.GRAY : ChatFormatting.RED)
                            .applyFormat(ChatFormatting.BOLD)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log death " + playerName + " " + limit + " " + (offset - limit)))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Previous Page")))
                    );
            MutableComponent nextButton = new TextComponent(" >>")
                    .setStyle(Style.EMPTY
                            .applyFormat(hitEndOfLogs ? ChatFormatting.GRAY : ChatFormatting.GREEN)
                            .applyFormat(ChatFormatting.BOLD)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/admin log death " + playerName + " " + limit + " " + (offset + limit)))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Next Page")))
                    );
            component.append(previousButton);
            component.append("-----");
            component.append(nextButton);
            source.sendSuccess(component, false);
        } catch (Exception e) {
            LOGGER.error("Error getting death logs", e);
            LOGGER.error(e.fillInStackTrace());
        }
        return 1;
    }

    private int getSingleDeath(CommandSourceStack source, Collection<ServerPlayer> targets, int deathId) throws CommandSyntaxException {
        // We basically wanna open a gui for the player to see the items for this death
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.players.too_many"));
            return 0;
        }
        // Get the player
        ServerPlayer player = targets.iterator().next();
        // Get their UUID
        String playerUUID = player.getStringUUID();
        String playerName = player.getName().getString();
        DeathEventObject deathEventObject = SwarmsmpS2.sqlite.getPlayerDeath(playerUUID, deathId);
        ServerPlayer sourcePlayer = source.getPlayerOrException();
        if (deathEventObject == null) {
            return 0;
        }
        BlockPos deathPos = deathEventObject.getPos();
        sourcePlayer.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return new TextComponent(player.getName().getString() + "'s Death Items");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
                return new ChestMenu(MenuType.GENERIC_9x5, pContainerId, pInventory, new DeadPlayerInventory(deathEventObject.getItems()), 5);
            }
        });
        return 1;
    }

    private BlockPos[] getBlockPositionsAround(BlockPos bPos, int scale) {
        // We need to get blocks around the position given to us, the scale is the radius of the cube we want to get
        // bPos is the center of our cube, so we need to get the blocks around it
        // We'll start with the bottom left corner, then go to the top right corner
        // We'll need to get the x, y, and z of the bottom left corner
        int x = bPos.getX() - scale;
        int y = bPos.getY() - scale;
        int z = bPos.getZ() - scale;
        // We'll need to get the x, y, and z of the top right corner
        int x2 = bPos.getX() + scale;
        int y2 = bPos.getY() + scale;
        int z2 = bPos.getZ() + scale;
        // We'll need to get the total number of blocks in our cube
        int totalBlocks = ((x2 - x) + 1) * ((y2 - y) + 1) * ((z2 - z) + 1);
        // Create our array of BlockPos
        BlockPos[] blockPositions = new BlockPos[totalBlocks];
        // Iterate through our cube
        int i = 0;
        for (int x3 = x; x3 <= x2; x3++) {
            for (int y3 = y; y3 <= y2; y3++) {
                for (int z3 = z; z3 <= z2; z3++) {
                    // Create our BlockPos
                    BlockPos blockPos = new BlockPos(x3, y3, z3);
                    // Add it to our array
                    blockPositions[i] = blockPos;
                    i++;
                }
            }
        }
        // Return our array
        return blockPositions;
    }

    private int getPlayerEffects(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.players.too_many"));
            return 0;
        }
        // Get the player
        ServerPlayer player = targets.iterator().next();
        // Get the effects
        Collection<MobEffectInstance> effects = player.getActiveEffects();
        // Convert the effects into a comma separated list
        String effectList = effects.stream().map(effect -> effect.getEffect().getDisplayName().getString()).collect(Collectors.joining(", "));
        // Send the effects to the player
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.players.effects.list", player.getDisplayName(), effectList), false);
        return 1;
    }

    private int getPlayerTeam(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.players.too_many"));
            return 0;
        }
        // Get the player
        ServerPlayer player = targets.iterator().next();
        // Get the team
        Team team = player.getTeam();
        // Send the team to the player
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.players.team.get", player.getDisplayName(), team == null ? "None" : team.getName()), false);
        return 1;
    }

    private int getPlayerHealth(CommandSourceStack source, Collection<ServerPlayer> targets) {
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.players.too_many"));
            return 0;
        }
        // Get the player
        ServerPlayer player = targets.iterator().next();
        // Get the health
        float health = player.getHealth();
        float hearts = health / 2;
        // Round hearts to 1 decimal place
        hearts = Math.round(hearts * 10.0) / 10.0f;
        // Send the health to the player
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.players.health.get", player.getDisplayName(), health, hearts), false);
        return 1;
    }

    private int setPlayerHealth(CommandSourceStack source, Collection<ServerPlayer> targets, int health) {
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.players.too_many"));
            return 0;
        }
        // Get the player
        ServerPlayer player = targets.iterator().next();
        // Set the health
        player.setHealth(health);
        // Send the health to the player
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.players.health.set", player.getDisplayName(), health), true);
        return 1;
    }

    private int sendMessageToPlayer(CommandSourceStack source, Collection<ServerPlayer> targets, Component message) throws CommandSyntaxException {
        // Make sure there is only one recipient
        if (targets.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.message.players.too_many"));
            return 0;
        }

        ServerPlayer recipient = targets.iterator().next();
        AdminMessage adminMessage = createAdminMessage(source.getServer(), source.getPlayerOrException(), recipient);
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.message.sent", recipient.getName(), message), false);
        recipient.sendMessage(new TranslatableComponent(translationKey + "commands.message.received", source.getPlayerOrException().getName(), message).withStyle(ChatFormatting.AQUA), DUMMY);
        MutableComponent textComponent = new TextComponent("Click here to reply").setStyle(Style.EMPTY
                .applyFormat(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/reply " + adminMessage.id))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponent("Click to reply to this message")))
        );
        recipient.sendMessage(textComponent, DUMMY);
        return 1;
    }

    private int getCoords(CommandSourceStack source) throws CommandSyntaxException {
        // Tell the player their own coordinates
        ServerPlayer player = source.getPlayerOrException();
        // Round our coords to 2 decimal places
        double x = Math.round(player.getX() * 100.0) / 100.0;
        double y = Math.round(player.getY() * 100.0) / 100.0;
        double z = Math.round(player.getZ() * 100.0) / 100.0;
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.self.coords", x, y, z), false);
        return 1;
    }

    private int getBiome(CommandSourceStack source) throws CommandSyntaxException {
        // Tell the player the biome they are in, needs to be the resource location name
        ServerPlayer player = source.getPlayerOrException();
        Holder<Biome> biome = player.getLevel().getBiome(player.blockPosition());
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.self.biome", biome.value().getRegistryName()), false);
        return 1;
    }

    private int addUUIDToBypass(CommandSourceStack source, String uuid) {
        // Get our current list of bypassed UUIDs from the config
        List<? extends String> bypassedUUIDs = SSMPS2Config.SERVER.bypassedPlayers.get();
        // Check if the UUID is already in the list
        if (bypassedUUIDs.contains(uuid)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.config.bypass.error.already_bypassed", uuid));
            return 0;
        }
        // We'll need to make a new list to add the UUID to
        List<String> newBypassedUUIDs = new ArrayList<>(bypassedUUIDs);
        newBypassedUUIDs.add(uuid);
        // Update the config
        SSMPS2Config.SERVER.bypassedPlayers.set(newBypassedUUIDs);
        SSMPS2Config.SERVER.bypassedPlayers.save();
        // Inform the user
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.config.bypass.add", uuid), true);
        return 1;
    }

    private int removeUUIDFromBypass(CommandSourceStack source, String uuid) {
        // Get our current list
        List<? extends String> bypassedUUIDs = SSMPS2Config.SERVER.bypassedPlayers.get();
        // Check if the UUID is in the list
        if (!bypassedUUIDs.contains(uuid)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.config.bypass.error.not_bypassed", uuid));
            return 0;
        }
        // We'll need to make a new list to remove the UUID from
        List<String> newBypassedUUIDs = new ArrayList<>(bypassedUUIDs);
        newBypassedUUIDs.remove(uuid);
        // Update the config
        SSMPS2Config.SERVER.bypassedPlayers.set(newBypassedUUIDs);
        SSMPS2Config.SERVER.bypassedPlayers.save();
        // Inform the user
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.config.bypass.remove", uuid), true);
        return 1;
    }

    private int getFactionSpawnpoint(CommandSourceStack source, String spawnpoint) {
        // Check if the faction is in the valid list
        if (!FACTIONS.contains(spawnpoint)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.error.invalid_faction", spawnpoint));
            return 0;
        }
        // Get the spawn point for the faction
        List<? extends Integer> spawnPoint = getSpawnpointForFaction(spawnpoint);

        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.config.spawnpoint.get", spawnpoint, spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2)), false);
        return 1;
    }

    private int setFactionSpawnpoint(CommandSourceStack source, String faction, Coordinates pPosition) throws CommandSyntaxException{
        // Check if the faction is in the valid list
        if (!FACTIONS.contains(faction)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.error.invalid_faction", faction));
            return 0;
        }
        Vec3 vec3 = pPosition.getPosition(source);
        // Create a list to store the spawn point
        List<Integer> spawnPoint = new ArrayList<>();
        // Add the coordinates to the list
        spawnPoint.add((int) vec3.x);
        spawnPoint.add((int) vec3.y);
        spawnPoint.add((int) vec3.z);
        SSMPS2Config.setSpawnpointForFaction(faction, spawnPoint);
        // Inform the user
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.config.spawnpoint.set", faction, spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2)), true);
        return 1;
    }

    private int getTagsForItem(CommandSourceStack source) throws CommandSyntaxException {
        // Get the player
        ServerPlayer player = source.getPlayerOrException();
        // Get the item in the player's main hand
        ItemStack item = player.getMainHandItem();
        // Check if their hand is empty
        if (item.isEmpty()) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.items.get_tags.empty_hand").withStyle(ChatFormatting.RED));
            return 0;
        }
        // Get the tags for the item they're holding
        Stream<TagKey<Item>> tags = item.getTags();
        // Convert the tags to a list
        List<TagKey<Item>> tagList = tags.toList();
        // Check if the item has any tags
        if (tagList.isEmpty()) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.items.get_tags.no_tags").withStyle(ChatFormatting.RED));
            return 0;
        }
        // Send the tags in a list to the player
        StringBuilder tagsToSend = new StringBuilder();
        for (TagKey<Item> tag : tagList) {
            tagsToSend.append(tag.location()).append(", ");
        }
        // Remove the last comma and space
        tagsToSend.delete(tagsToSend.length() - 2, tagsToSend.length());
        // Send it to the player
        player.sendMessage(new TranslatableComponent(translationKey + "commands.admin.items.get_tags.success", tagsToSend.toString()).withStyle(ChatFormatting.AQUA), Util.NIL_UUID);
        return 1;
    }

    private int giveHeadOfPlayer(CommandSourceStack source, Collection<ServerPlayer> targets) throws CommandSyntaxException {
        int howMuchHeadTheyGot = 0;
        for (ServerPlayer target : targets) {
            ItemStack headItem = EntityHelpers.getPlayerHead(target);

            boolean success = source.getPlayerOrException().getInventory().add(headItem);
            if (!success) {
                source.getPlayerOrException().spawnAtLocation(headItem);
            }
            howMuchHeadTheyGot++;
        }
        if (howMuchHeadTheyGot == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.success").withStyle(ChatFormatting.AQUA), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.success.multiple", howMuchHeadTheyGot)
                            .withStyle(ChatFormatting.AQUA)
                            .withStyle(ChatFormatting.BOLD)
                            .withStyle(ChatFormatting.ITALIC)
                            .withStyle(ChatFormatting.UNDERLINE),
                    true);
        }
        return 1;
    }

    private int giveHeadOfPlayerToPlayer(CommandSourceStack source, Collection<ServerPlayer> targets, Collection<ServerPlayer> headTargets) throws CommandSyntaxException {
        int howMuchHeadTheyGot = 0;

        // Make sure headTargets is only one player
        if (headTargets.size() > 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.head.failure.multiple_heads"));
            return 0;
        }

        ServerPlayer headTarget = headTargets.iterator().next();

        for (ServerPlayer target : targets) {
            ItemStack headItem = EntityHelpers.getPlayerHead(target);

            boolean success = headTarget.getInventory().add(headItem);
            if (!success) {
                headTarget.spawnAtLocation(headItem);
            }
            howMuchHeadTheyGot++;
        }
        if (howMuchHeadTheyGot == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.sent_success").withStyle(ChatFormatting.AQUA), true);
            headTarget.sendMessage(new TranslatableComponent(translationKey + "commands.admin.head.success").withStyle(ChatFormatting.AQUA), Util.NIL_UUID);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.head.sent_success.multiple", howMuchHeadTheyGot)
                            .withStyle(ChatFormatting.AQUA)
                            .withStyle(ChatFormatting.BOLD)
                            .withStyle(ChatFormatting.ITALIC)
                            .withStyle(ChatFormatting.UNDERLINE),
                    true);
            headTarget.sendMessage(new TranslatableComponent(translationKey + "commands.admin.head.success.multiple", howMuchHeadTheyGot)
                            .withStyle(ChatFormatting.AQUA)
                            .withStyle(ChatFormatting.BOLD)
                            .withStyle(ChatFormatting.ITALIC)
                            .withStyle(ChatFormatting.UNDERLINE),
                    Util.NIL_UUID);
        }
        return 1;
    }

    private int setTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag) {
        for (ServerPlayer target : targets) {
            target.getPersistentData().putBoolean(SwarmsmpS2.MODID + ":" + tag, true);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.set_tag.success.single", tag, targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.set_tag.success.multiple", tag, targets.size()), true);
        }
        return 1;
    }

    private int removeTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag) {
        for (ServerPlayer target : targets) {
            target.getPersistentData().remove(SwarmsmpS2.MODID + ":" + tag);
        }

        if (targets.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.remove_tag.success.single", tag, targets.iterator().next().getDisplayName()), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.remove_tag.success.multiple", tag, targets.size()), true);
        }
        return 1;
    }

    private int checkTag(CommandSourceStack source, Collection<ServerPlayer> targets, String tag) {
        for (ServerPlayer target : targets) {
            if (target.getPersistentData().contains(SwarmsmpS2.MODID + ":" + tag)) {
                source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.check_tag.success", target.getDisplayName()), true);
            } else {
                source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.check_tag.failed", target.getDisplayName()), true);
            }
        }
        return 1;
    }

    private int startDuel(CommandSourceStack source, Collection<ServerPlayer> firstPlayers, Collection<ServerPlayer> secondPlayers) {
        // Make sure both collections are only 1 player
        if (firstPlayers.size() != 1 || secondPlayers.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }

        ServerPlayer firstPlayer = firstPlayers.iterator().next();
        ServerPlayer secondPlayer = secondPlayers.iterator().next();

        // Player's data
        CompoundTag firstPlayerData = firstPlayer.getPersistentData();
        CompoundTag secondPlayerData = secondPlayer.getPersistentData();

        // Make sure they're not the same player
        if (firstPlayer.getUUID().equals(secondPlayer.getUUID())) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.start_duel.error.same_player"));
            return 0;
        }

        // Make sure they're both not already in a duel
        boolean firstPlayerInDuel = firstPlayerData.contains(SwarmsmpS2.MODID + ":dueling");
        boolean secondPlayerInDuel = secondPlayerData.contains(SwarmsmpS2.MODID + ":dueling");
        if (firstPlayerInDuel || secondPlayerInDuel) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.start_duel.error.already_in_duel"));
            return 0;
        }

        boolean success = DuelHelper.createDuelBetweenPlayers(firstPlayer, secondPlayer, true);

        if (success) {
            // Inform all players of the duel
            List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
            players.forEach(player -> {
                player.sendMessage(new TranslatableComponent(
                        translationKey + "commands.admin.start_duel.inform_server",
                        firstPlayer.getDisplayName(),
                        secondPlayer.getDisplayName()
                ).withStyle(ChatFormatting.AQUA), DUMMY);
            });
        }

        return 1;
    }

    private int endDuel(CommandSourceStack source, Collection<ServerPlayer> player) {
        // Ensure the collection is just 1 player
        if (player.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }
        // Check if they're in a duel
        ServerPlayer playerInDuel = player.iterator().next();
        boolean isInDuel = playerInDuel.getPersistentData().contains(SwarmsmpS2.MODID + ":dueling");
        if (!isInDuel) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.end_duel.error.not_in_duel"));
            return 0;
        }

        // Player must be in a duel so we have our first player
        // We'll need to check their "duel_target" persistent data to get the second player
        UUID secondPlayerUUID = playerInDuel.getPersistentData().getUUID(SwarmsmpS2.MODID + ":duel_target");
        // Get a player from this UUID
        ServerPlayer secondPlayer = source.getServer().getPlayerList().getPlayer(secondPlayerUUID);
        // End the duel
        DuelHelper.endDuelBetweenPlayers(playerInDuel, secondPlayer, true);

        return 1;
    }

    private int getPlayerDeathCount(CommandSourceStack source, Collection<ServerPlayer> players) {
        LOGGER.debug("Command init");
        if (players.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }

        ServerPlayer playerToCheck = players.iterator().next();
        LOGGER.debug("Checking death count for " + playerToCheck.getDisplayName().getString());
        // Get their persistent data
        CompoundTag playerData = playerToCheck.getPersistentData();
        LOGGER.debug("Got persistent data");
        // Get their death count
        int deathCount = playerData.getInt(SwarmsmpS2.MODID + ":death_count");
        LOGGER.debug("Death count for " + playerToCheck.getDisplayName().getString() + " is " + deathCount);

        // Tell the initiator of the command the death count
        if (deathCount == 0) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.get_deaths.none", playerToCheck.getDisplayName()), false);
        } else if (deathCount == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.get_deaths", playerToCheck.getDisplayName(), deathCount), false);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.get_deaths.multiple", playerToCheck.getDisplayName(), deathCount), false);
        }

        return 1;
    }

    private int setPlayerDeathCount(CommandSourceStack source, Collection<ServerPlayer> players, int deathCount) {
        if (players.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }

        ServerPlayer playerToSet = players.iterator().next();
        // Get their persistent data
        CompoundTag playerData = playerToSet.getPersistentData();
        // Set their death count
        playerData.putInt(SwarmsmpS2.MODID + ":death_count", deathCount);

        // Tell the initiator of the command the death count
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.set_deaths", playerToSet.getDisplayName(), deathCount), false);
        return 1;
    }

    private int resetPlayerDeathCount(CommandSourceStack source, Collection<ServerPlayer> players) {
        if (players.size() != 1) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.error.players"));
            return 0;
        }

        ServerPlayer playerToReset = players.iterator().next();
        // Get their persistent data
        CompoundTag playerData = playerToReset.getPersistentData();
        // Reset their death count
        playerData.putInt(SwarmsmpS2.MODID + ":death_count", 0);

        // Tell the initiator
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.reset_deaths", playerToReset.getDisplayName()), false);
        return 1;
    }

    private int teleportToFactionSpawn(CommandSourceStack source, String faction) throws CommandSyntaxException {
        // Check if the faction is in the valid list
        if (!FACTIONS.contains(faction)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.error.invalid_faction", faction));
            return 0;
        }
        // Get the invoking player
        ServerPlayer player = source.getPlayerOrException();
        List<? extends Integer> spawnPoint = getSpawnpointForFaction(faction);

        // Now we just teleport them to the spawn point
        player.teleportTo(spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2));
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn", player.getDisplayName(), faction), true);
        return 1;
    }

    private int teleportPlayersToFactionSpawn(CommandSourceStack source, String faction, Collection<ServerPlayer> players) {
        // Check if the faction is in the valid list
        if (!FACTIONS.contains(faction)) {
            source.sendFailure(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.error.invalid_faction", faction));
            return 0;
        }
        // Get the spawn point for the faction
        List<? extends Integer> spawnPoint = getSpawnpointForFaction(faction);
        // Loop through the players and teleport them
        players.forEach(player -> {
            player.teleportTo(spawnPoint.get(0), spawnPoint.get(1), spawnPoint.get(2));
        });

        if (players.size() == 1) {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn", players.iterator().next().getDisplayName(), faction), true);
        } else {
            source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.teleport_to_spawn.multiple", players.size(), faction), true);
        }
        return 1;
    }

    private int reloadConfigFile(CommandSourceStack source) {
        // Send an OnConfigChangeEvent to the event bus
        SSMPS2Config.serverSpec.afterReload();
        source.sendSuccess(new TranslatableComponent(translationKey + "commands.admin.reload_config"), true);
        return 1;
    }

    private int triggerSAOMode(CommandSourceStack source, boolean state) {
        SaoModePacket packet = new SaoModePacket(state);
        SwarmSMPPacketHandler.SAO_MODE_CHANNEL.send(PacketDistributor.ALL.noArg(), packet);
        source.sendSuccess(new TextComponent("Link start!"), true);
        return 1;
    }

    private int triggerSAOMode(CommandSourceStack source, boolean state, Collection<ServerPlayer> players) {
        SaoModePacket packet = new SaoModePacket(state);
        SwarmSMPPacketHandler.SAO_MODE_CHANNEL.send(PacketDistributor.PLAYER.with(() -> players.iterator().next()), packet);
        source.sendSuccess(new TextComponent("Link start!"), true);
        return 1;
    }
}
