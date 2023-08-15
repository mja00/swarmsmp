package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.objects.BlockEventObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.CapabilityItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class LogBlockEvents {

    static Logger LOGGER = LogManager.getLogger("LOGBLOCKEVENTS");
    static final ExecutorService exe = Executors.newCachedThreadPool();

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        // If we're client side just return
        if (event.getWorld().isClientSide()) {
            return;
        }
        // Make sure it's a player breaking the block
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        if (player == null) {
            return;
        }
        // Get the block broken
        Block block = event.getState().getBlock();
        BlockPos bPos = event.getPos();
        String blockRegistryName = block.getRegistryName() != null ? block.getRegistryName().toString() : "unknown";
        BlockEventObject blockEvent = new BlockEventObject(player.getStringUUID(), blockRegistryName, "block_break", bPos.getX(), bPos.getY(), bPos.getZ(), 0);
        // Log it
        if (SSMPS2Config.SERVER.logToConsole.get()) { LOGGER.info(player.getName().getString() + " broke " + blockRegistryName + " at " + bPos.getX() + ", " + bPos.getY() + ", " + bPos.getZ() + "."); }
        if (SwarmsmpS2.sqlite == null) { return; }
        exe.execute(() -> {
            SwarmsmpS2.sqlite.createWorldEvent(blockEvent);
        });
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        // Check if not instanceof Player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        // Make sure it's a player placing the block
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (player == null) {
            return;
        }
        // Get the block placed
        Block block = event.getPlacedBlock().getBlock();
        BlockPos bPos = event.getPos();
        String blockRegistryName = block.getRegistryName() != null ? block.getRegistryName().toString() : "unknown";
        BlockEventObject blockEvent = new BlockEventObject(player.getStringUUID(), blockRegistryName, "block_place", bPos.getX(), bPos.getY(), bPos.getZ(), 0);
        // Log it
        if (SSMPS2Config.SERVER.logToConsole.get()) { LOGGER.info(player.getName().getString() + " placed " + blockRegistryName + " at " + bPos.getX() + ", " + bPos.getY() + ", " + bPos.getZ() + "."); }
        if (SwarmsmpS2.sqlite == null) { return; }
        exe.execute(() -> {
            SwarmsmpS2.sqlite.createWorldEvent(blockEvent);
        });
    }

    @SubscribeEvent
    public static void onFarmlandTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        // Check if not instanceof Player
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        // Make sure it's a player placing the block
        ServerPlayer player = (ServerPlayer) event.getEntity();
        if (player == null) {
            return;
        }
        // Get the target pos
        BlockPos bPos = event.getPos();
        Block block = event.getState().getBlock();
        String blockRegistryName = block.getRegistryName() != null ? block.getRegistryName().toString() : "unknown";
        BlockEventObject blockEvent = new BlockEventObject(player.getStringUUID(), blockRegistryName, "farmland_trample", bPos.getX(), bPos.getY(), bPos.getZ(), 0);
        // Log it
        if (SSMPS2Config.SERVER.logToConsole.get()) { LOGGER.info(player.getName().getString() + " trampled farmland at " + bPos.getX() + ", " + bPos.getY() + ", " + bPos.getZ() + "."); }
        if (SwarmsmpS2.sqlite == null) { return; }
        exe.execute(() -> {
            SwarmsmpS2.sqlite.createWorldEvent(blockEvent);
        });
    }

    @SubscribeEvent
    public static void onBucketUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        ItemStack bucket = event.getItemStack();
        ResourceLocation registryName = bucket.getItem().getRegistryName();
        if (registryName == null) {
            return;
        }
        // Check if it's any type of bucket
        if (!registryName.getPath().contains("bucket") || registryName.getPath().contains("milk")) {
            return;
        }
        // Make sure it's a player using the bucket
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        if (player == null) {
            return;
        }
        // Get the target pos
        BlockPos bPos = event.getPos();
        BlockEventObject blockEvent = new BlockEventObject(player.getStringUUID(), bucket.getHoverName().getString(), "bucket_use", bPos.getX(), bPos.getY(), bPos.getZ(), 0);
        // Log it
        if (SSMPS2Config.SERVER.logToConsole.get()) { LOGGER.info(player.getName().getString() + " used a " + bucket.getHoverName().getString() + " at " + bPos.getX() + ", " + bPos.getY() + ", " + bPos.getZ() + "."); }
        if (SwarmsmpS2.sqlite == null) { return; }
        exe.execute(() -> {
            SwarmsmpS2.sqlite.createWorldEvent(blockEvent);
        });
    }

    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getWorld().isClientSide()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        if (player == null) {
            return;
        }
        // If the player is in spectator we don't care
        if (player.isSpectator()) {
            return;
        }
        // Get the block right clicked
        BlockPos bPos = event.getPos();
        BlockState bState = event.getWorld().getBlockState(bPos);
        Block block = bState.getBlock();
        BlockEntity bEntity = event.getWorld().getBlockEntity(bPos);
        if (bEntity == null ) {
            return;
        }
        if (bEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent()) {
            // It has the ability to hold items, so we should log it
            String blockRegistryName = block.getRegistryName() != null ? block.getRegistryName().toString() : "unknown";
            BlockEventObject blockEvent = new BlockEventObject(player.getStringUUID(), blockRegistryName, "block_right_click", bPos.getX(), bPos.getY(), bPos.getZ(), 0);
            // Log it
            if (SSMPS2Config.SERVER.logToConsole.get()) { LOGGER.info(player.getName().getString() + " right clicked " + blockRegistryName + " at " + bPos.getX() + ", " + bPos.getY() + ", " + bPos.getZ() + "."); }
            if (SwarmsmpS2.sqlite == null) { return; }
            exe.execute(() -> {
                SwarmsmpS2.sqlite.createWorldEvent(blockEvent);
            });
        }
    }

//    @SubscribeEvent
//    public static void debugBlockEvent(BlockEvent event) {
//        LOGGER.info(event.toString());
//    }
}
