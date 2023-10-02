package dev.mja00.swarmsmps2.events.curses;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.helpers.EntityHelpers;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SwarmsmpS2.MODID)
public class FoodCurse {

    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        // Check that its a player
        LivingEntity entity = event.getEntityLiving();
        if (!(entity instanceof ServerPlayer player)) return;
        Item item = event.getItem().getItem();
        if (!EntityHelpers.playerHasTag(entity.getPersistentData(), "sweetberry_curse")) return;
        // Check that they're eating sweet berries
        if (item == Items.SWEET_BERRIES) return;
        // Are they consuming a food?
        // We'll check the item for the food tag
        if (item.isEdible()) {
            // Reverse the effects of the food
            FoodProperties foodProperties = item.getFoodProperties(event.getItem(), entity);
            if (foodProperties != null) {
                int foodNutrition = foodProperties.getNutrition();
                float foodSaturation = foodProperties.getSaturationModifier();
                // Subtract both the food and sat from the player
                player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - foodNutrition);
                player.getFoodData().setSaturation(player.getFoodData().getSaturationLevel() - (foodSaturation * foodNutrition));
            }
            // Change the item to sweet berries
            ItemStack result = event.getResultStack();
            int count = result.getCount();
            ItemStack sweetBerries = new ItemStack(Items.SWEET_BERRIES, count);
            event.setResultStack(sweetBerries);
            player.sendMessage(new TextComponent("The food in your hand morphs into sweet berries. What you just ate tasted like rotten wood.").withStyle(ChatFormatting.DARK_PURPLE), Util.NIL_UUID);
        }
    }
}
