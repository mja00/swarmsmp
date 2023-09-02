package dev.mja00.swarmsmps2.client.gui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScreenOpener {
    public static void openSendingStoneScreen(Player pPlayer, ItemStack mainhand) {
        Minecraft.getInstance().setScreen(new SendingStoneScreen(pPlayer, mainhand));
    }
}
