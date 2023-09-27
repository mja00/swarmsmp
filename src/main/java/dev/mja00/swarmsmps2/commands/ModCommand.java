package dev.mja00.swarmsmps2.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.config.FeyConfig;
import dev.mja00.swarmsmps2.objects.PlayerObject;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

public class ModCommand {

    static Logger LOGGER = SwarmsmpS2.LOGGER;
    static final UUID DUMMY = Util.NIL_UUID;
    private static final SimpleCommandExceptionType INVALID_POSITION = new SimpleCommandExceptionType(new TranslatableComponent("commands.teleport.invalidPosition"));

    public ModCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> literalArgumentBuilder = Commands.literal("ssmpmod").requires(this::isModerator);

        for (GameType gametype : GameType.values()) {
            if (gametype.isCreative()) {
                continue;
            }
            literalArgumentBuilder.then(Commands.literal(gametype.getName()).executes((command) -> changeGamemode(command.getSource(), gametype)));
        }

        literalArgumentBuilder.then(Commands.literal("tp").then(Commands.argument("player", EntityArgument.player())
                .executes((command) -> teleportToPlayer(command.getSource(), Collections.singleton(command.getSource().getEntityOrException()), EntityArgument.getPlayer(command, "player")))));

        literalArgumentBuilder.then(Commands.literal("fly").executes((command) -> toggleFlight(command.getSource())));

        literalArgumentBuilder.then(Commands.literal("fey").then(Commands.literal("on").executes((command) -> becomeFey(command.getSource())))
                .then(Commands.literal("off").executes((command) -> leaveFey(command.getSource()))));

        dispatcher.register(literalArgumentBuilder);
    }

    private boolean isModerator(CommandSourceStack source) {
        if (source.hasPermission(2)) {
            return true;
        }
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            return false;
        }
        UUID playerUUID = player.getUUID();
        List<UUID> modUUIDs = SSMPS2Config.getModsFromFile();
        return modUUIDs.contains(playerUUID);
    }

    private int changeGamemode(CommandSourceStack source, GameType gamemode) throws CommandSyntaxException{
        ServerPlayer player = source.getPlayerOrException();
        player.setGameMode(gamemode);
        source.sendSuccess(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.ssmpmod.gamemode.success", gamemode.getName()), true);
        return 1;
    }

    private int teleportToPlayer(CommandSourceStack pSource, Collection<? extends Entity> pTargets, Entity pDestination) throws CommandSyntaxException {
        for (Entity entity : pTargets) {
            performTeleport(pSource, entity, (ServerLevel) pDestination.level, pDestination.getX(), pDestination.getY(), pDestination.getZ(), EnumSet.noneOf(ClientboundPlayerPositionPacket.RelativeArgument.class), pDestination.getYRot(), pDestination.getXRot(), null);
        }
        pSource.sendSuccess(new TranslatableComponent("commands.teleport.success.entity.single", pTargets.iterator().next().getDisplayName(), pDestination.getDisplayName()), true);
        return 1;
    }

    private static void performTeleport(CommandSourceStack pSource, Entity pEntity, ServerLevel pLevel, double pX, double pY, double pZ, Set<ClientboundPlayerPositionPacket.RelativeArgument> pRelativeList, float pYaw, float pPitch, @Nullable ModCommand.LookAt pFacing) throws CommandSyntaxException {
        net.minecraftforge.event.entity.EntityTeleportEvent.TeleportCommand event = net.minecraftforge.event.ForgeEventFactory.onEntityTeleportCommand(pEntity, pX, pY, pZ);
        if (event.isCanceled()) return;
        pX = event.getTargetX(); pY = event.getTargetY(); pZ = event.getTargetZ();
        BlockPos blockpos = new BlockPos(pX, pY, pZ);
        if (!Level.isInSpawnableBounds(blockpos)) {
            throw INVALID_POSITION.create();
        } else {
            float f = Mth.wrapDegrees(pYaw);
            float f1 = Mth.wrapDegrees(pPitch);
            if (pEntity instanceof ServerPlayer) {
                ChunkPos chunkpos = new ChunkPos(new BlockPos(pX, pY, pZ));
                pLevel.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkpos, 1, pEntity.getId());
                pEntity.stopRiding();
                if (((ServerPlayer)pEntity).isSleeping()) {
                    ((ServerPlayer)pEntity).stopSleepInBed(true, true);
                }

                if (pLevel == pEntity.level) {
                    ((ServerPlayer)pEntity).connection.teleport(pX, pY, pZ, f, f1, pRelativeList);
                } else {
                    ((ServerPlayer)pEntity).teleportTo(pLevel, pX, pY, pZ, f, f1);
                }

                pEntity.setYHeadRot(f);
            } else {
                float f2 = Mth.clamp(f1, -90.0F, 90.0F);
                if (pLevel == pEntity.level) {
                    pEntity.moveTo(pX, pY, pZ, f, f2);
                    pEntity.setYHeadRot(f);
                } else {
                    pEntity.unRide();
                    Entity entity = pEntity;
                    pEntity = pEntity.getType().create(pLevel);
                    if (pEntity == null) {
                        return;
                    }

                    pEntity.restoreFrom(entity);
                    pEntity.moveTo(pX, pY, pZ, f, f2);
                    pEntity.setYHeadRot(f);
                    entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
                    pLevel.addDuringTeleport(pEntity);
                }
            }

            if (pFacing != null) {
                pFacing.perform(pSource, pEntity);
            }

            if (!(pEntity instanceof LivingEntity) || !((LivingEntity)pEntity).isFallFlying()) {
                pEntity.setDeltaMovement(pEntity.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
                pEntity.setOnGround(true);
            }

            if (pEntity instanceof PathfinderMob) {
                ((PathfinderMob)pEntity).getNavigation().stop();
            }

        }
    }

    static class LookAt {
        private final Vec3 position;
        private final Entity entity;
        private final EntityAnchorArgument.Anchor anchor;

        public LookAt(Entity p_139056_, EntityAnchorArgument.Anchor p_139057_) {
            this.entity = p_139056_;
            this.anchor = p_139057_;
            this.position = p_139057_.apply(p_139056_);
        }

        public LookAt(Vec3 p_139059_) {
            this.entity = null;
            this.position = p_139059_;
            this.anchor = null;
        }

        public void perform(CommandSourceStack pSource, Entity pEntity) {
            if (this.entity != null) {
                if (pEntity instanceof ServerPlayer) {
                    ((ServerPlayer)pEntity).lookAt(pSource.getAnchor(), this.entity, this.anchor);
                } else {
                    pEntity.lookAt(pSource.getAnchor(), this.position);
                }
            } else {
                pEntity.lookAt(pSource.getAnchor(), this.position);
            }

        }
    }

    private int toggleFlight(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Abilities abilities = player.getAbilities();
        boolean isFlying = abilities.mayfly;
        abilities.mayfly = !isFlying;
        player.onUpdateAbilities();
        if (!isFlying) {
            player.displayClientMessage(new TextComponent("Flight enabled"), true);
        } else {
            // We also wanna set flying to false, otherwise the player will still be flying
            abilities.flying = false;
            player.displayClientMessage(new TextComponent("Flight disabled"), true);
        }
        return 1;
    }

    private int becomeFey(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String playerUUID = player.getUUID().toString();
        if (FeyConfig.doesFeyExist(playerUUID)) {
            source.sendFailure(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.ssmpmod.fey.already_fey"));
            return 0;
        }
        BlockPos playerPos = player.blockPosition();
        long timestamp = System.currentTimeMillis();
        int playerFood = player.getFoodData().getFoodLevel();
        float playerSaturation = player.getFoodData().getSaturationLevel();
        float playerHealth = player.getHealth();
        List<ItemStack> items = player.getInventory().items;
        List<ItemStack> armor = player.getInventory().armor;
        List<ItemStack> offhand = player.getInventory().offhand;
        // Create a new PlayerObject
        PlayerObject fey = new PlayerObject(playerUUID, playerPos, timestamp, playerFood, playerHealth, playerSaturation, items, armor, offhand);
        try {
            FeyConfig.writeNewFey(fey);
            // Clear their inventory and make them spectator
            player.getInventory().clearContent();
            player.setGameMode(GameType.SPECTATOR);
            // Make them fly
            Abilities abilities = player.getAbilities();
            abilities.mayfly = true;
            player.onUpdateAbilities();
            source.sendSuccess(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.ssmpmod.fey.success"), true);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Failed to write new fey to file: " + e.getMessage());
            source.sendFailure(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.ssmpmod.fey.failed_to_write_to_file", e.getMessage()));
            return 0;
        }
    }

    private int leaveFey(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        String playerUUID = player.getUUID().toString();
        if (!FeyConfig.doesFeyExist(playerUUID)) {
            source.sendFailure(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.ssmpmod.fey.not_fey"));
            return 0;
        }
        PlayerObject fey = FeyConfig.getFeyFromPlayerUUID(playerUUID);
        if (fey == null) {
            source.sendFailure(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.ssmpmod.fey.failed_to_get_fey"));
            return 0;
        }
        // Set their food, saturation, and health
        player.getFoodData().setFoodLevel(fey.getPlayerFood());
        player.getFoodData().setSaturation(fey.getPlayerSaturation());
        player.setHealth(fey.getPlayerHealth());
        // Set their inventory
        player.getInventory().clearContent();
        // Set main inventory
        for (int i = 0; i < fey.getItems().size(); i++) {
            player.getInventory().items.set(i, fey.getItems().get(i));
        }

        // Set armor
        for (int i = 0; i < fey.getArmor().size(); i++) {
            player.getInventory().armor.set(i, fey.getArmor().get(i));
        }

        // Set offhand
        for (int i = 0; i < fey.getOffhand().size(); i++) {
            player.getInventory().offhand.set(i, fey.getOffhand().get(i));
        }

        // Set their position
        BlockPos pos = fey.getLastPos();
        player.teleportTo(pos.getX(), pos.getY(), pos.getZ());
        // Set their gamemode
        player.setGameMode(GameType.SPECTATOR);
        // Remove them from the fey file
        try {
            FeyConfig.removeFey(playerUUID);
        } catch (Exception e) {
            LOGGER.error("Failed to remove fey from file: " + e.getMessage());
            source.sendFailure(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.ssmpmod.fey.failed_to_remove_from_file", e.getMessage()));
            return 0;
        }
        player.onUpdateAbilities();
        // Send them a success message
        source.sendSuccess(new TranslatableComponent(SwarmsmpS2.translationKey + "commands.ssmpmod.fey.return.success"), true);
        return 1;
    }
}
