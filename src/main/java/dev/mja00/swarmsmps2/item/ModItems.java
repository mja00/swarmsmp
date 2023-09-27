package dev.mja00.swarmsmps2.item;

import dev.mja00.swarmsmps2.SSMPS2Config;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.item.custom.FaePoofItem;
import dev.mja00.swarmsmps2.item.custom.FlowerCrownItem;
import dev.mja00.swarmsmps2.item.custom.SendingStoneItem;
import dev.mja00.swarmsmps2.item.materials.FlowerCrownMaterial;
import net.minecraft.world.entity.EquipmentSlot;
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

    public static final RegistryObject<Item> FAE_POOF = ITEMS.register("fae_poof",
            () -> new FaePoofItem(new Item.Properties().tab(CreativeModeTab.TAB_MISC).stacksTo(1)));

    public static final RegistryObject<Item> FLOWER_CROWN = ITEMS.register("flower_crown",
            () -> new FlowerCrownItem(FlowerCrownMaterial.INSTANCE, EquipmentSlot.HEAD,
                    new Item.Properties().tab(CreativeModeTab.TAB_TOOLS)));

    public static final RegistryObject<Item> AZALEA_FLOWER = ITEMS.register("azalea_flower",
            () -> new Item(new Item.Properties().tab(CreativeModeTab.TAB_MISC)));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
