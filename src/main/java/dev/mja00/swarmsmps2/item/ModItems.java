package dev.mja00.swarmsmps2.item;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.item.custom.SendingStoneItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SwarmsmpS2.MODID);

    public static final RegistryObject<Item> SENDING_STONE = ITEMS.register("sending_stone",
            () -> new SendingStoneItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1).defaultDurability(SSMPS2Config.SERVER.sendingStoneDurability.get())));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
