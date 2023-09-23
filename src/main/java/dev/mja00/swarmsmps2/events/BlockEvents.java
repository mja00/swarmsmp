package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
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
    public static void onGrindstoneRightClick(PlayerInteractEvent.RightClickBlock event) {
        // ensure they're right clicking a grindstone
        if (event.getSide() != LogicalSide.SERVER) return;
        BlockPos clickedBlock = event.getPos();
        ResourceLocation blockName = event.getWorld().getBlockState(clickedBlock).getBlock().getRegistryName();
        if (blockName == null) return;
        if (!blockName.equals(new ResourceLocation("minecraft", "grindstone"))) return;
        // We have a grindstone
        // Check if the player is holding an iron ingot
        ItemStack heldItemStack = event.getItemStack();
        if (heldItemStack.equals(ItemStack.EMPTY)) return;
        Item heldItem = heldItemStack.getItem();
        if (!heldItem.equals(Items.IRON_INGOT)) return;
        // We have an iron ingot
        event.setCanceled(true);
        Player player = event.getPlayer();
        // Remove 1 iron ingot from the player's inventory
        // If the player is creative dont remove the item
        if (!player.isCreative()) {
            heldItemStack.shrink(1);
        }
        ItemStack steelWool = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("immersive_weathering", "steel_wool")));
        player.addItem(steelWool);
        // Play a sound to the player
        Level world = event.getWorld();
        world.playSound(null, clickedBlock.getX(), clickedBlock.getY(), clickedBlock.getZ(), SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
