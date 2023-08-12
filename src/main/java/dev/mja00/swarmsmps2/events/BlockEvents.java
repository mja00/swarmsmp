package dev.mja00.swarmsmps2.events;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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
}
