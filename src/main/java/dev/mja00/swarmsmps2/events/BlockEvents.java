package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class BlockEvents {

    static Logger LOGGER = SwarmsmpS2.LOGGER;

    @SubscribeEvent
    public static void onLavaSmelt(FurnaceFuelBurnTimeEvent event) {
        ItemStack itemStack = event.getItemStack();
        Item lavaBucket = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", "lava_bucket"));
        if (itemStack.getItem() == lavaBucket) {
            event.setBurnTime(2000);
        }

    }

    @SubscribeEvent
    public static void onAzaleaShear(PlayerInteractEvent.RightClickBlock event) {
        // If they're not holding shears we don't care
        if (event.getSide() != LogicalSide.SERVER) return;
        ItemStack heldItem = event.getItemStack();
        if (heldItem.getItem() != Items.SHEARS) return;
        Level level = event.getWorld();
        // If they're not right clicking a flowering azalea or flowering azalea leaves we don't care
        BlockPos clickedBlockPos = event.getPos();
        ResourceLocation blockName = level.getBlockState(clickedBlockPos).getBlock().getRegistryName();
        ResourceLocation floweringAzaleaLeaves = Blocks.FLOWERING_AZALEA_LEAVES.getRegistryName();
        ResourceLocation floweringAzalea = Blocks.FLOWERING_AZALEA.getRegistryName();
        if (blockName == null) return;
        if (!blockName.equals(floweringAzalea) && !blockName.equals(floweringAzaleaLeaves)) return;
        // Now we spawn an item on the ground
        event.setCanceled(true);
        Player player = event.getPlayer();
        // Swap the block to its now flowerless counterpart
        if (blockName.equals(floweringAzalea)) {
            level.setBlock(clickedBlockPos, Blocks.AZALEA.defaultBlockState(), 3);
        } else {
            level.setBlock(clickedBlockPos, Blocks.AZALEA_LEAVES.defaultBlockState(), 3);
        }
        // Spawn the item
        Block.popResourceFromFace(event.getWorld(), clickedBlockPos, event.getHitVec().getDirection(), new ItemStack(ModItems.AZALEA_FLOWER.get()));
        // Damage the shears by 1
        heldItem.hurtAndBreak(1, player, (playerEntity) -> {
            playerEntity.broadcastBreakEvent(event.getHand());
        });

        level.playSound(event.getPlayer(), clickedBlockPos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (player instanceof ServerPlayer) {
            // Up their stat
            player.awardStat(Stats.ITEM_USED.get(Items.SHEARS));
        }
    }

//    @SubscribeEvent
//    public static void onGrindstoneRightClick(PlayerInteractEvent.RightClickBlock event) {
//        // ensure they're right clicking a grindstone
//        if (event.getSide() != LogicalSide.SERVER) return;
//        BlockPos clickedBlock = event.getPos();
//        ResourceLocation blockName = event.getWorld().getBlockState(clickedBlock).getBlock().getRegistryName();
//        if (blockName == null) return;
//        if (!blockName.equals(new ResourceLocation("minecraft", "grindstone"))) return;
//        // We have a grindstone
//        // Check if the player is holding an iron ingot
//        ItemStack heldItemStack = event.getItemStack();
//        if (heldItemStack.equals(ItemStack.EMPTY)) return;
//        Item heldItem = heldItemStack.getItem();
//        if (!heldItem.equals(Items.IRON_INGOT)) return;
//        // We have an iron ingot
//        event.setCanceled(true);
//        Player player = event.getPlayer();
//        // Remove 1 iron ingot from the player's inventory
//        // If the player is creative dont remove the item
//        if (!player.isCreative()) {
//            heldItemStack.shrink(1);
//        }
//        ItemStack steelWool = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("immersive_weathering", "steel_wool")));
//        player.addItem(steelWool);
//        // Play a sound to the player
//        Level world = event.getWorld();
//        world.playSound(null, clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
//    }
}
