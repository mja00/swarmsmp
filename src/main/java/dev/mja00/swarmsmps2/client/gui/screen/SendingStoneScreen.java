package dev.mja00.swarmsmps2.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.network.SwarmSMPPacketHandler;
import dev.mja00.swarmsmps2.network.packets.SendingStonePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class SendingStoneScreen extends Screen {
    private ItemStack item;
    protected EditBox messageBox;
    private static final Component TITLE = new TranslatableComponent(SwarmsmpS2.translationKey + "gui.sending_stone.title");
    private static final Component LABEL = new TranslatableComponent(SwarmsmpS2.translationKey + "gui.sending_stone.message", 25);
    protected Button doneButton;
    protected Button cancelButton;
    protected Font font = Minecraft.getInstance().font;
    protected Minecraft minecraft = Minecraft.getInstance();
    private static final Logger LOGGER = LogManager.getLogger("SSMPS2/SendingStoneScreen");

    public SendingStoneScreen(Player player, ItemStack item) {
        super(NarratorChatListener.NO_TITLE);
        this.item = item;
    }

    @Override
    protected void init() {
        super.init();
        this.doneButton = this.addRenderableWidget(new Button(this.width / 2 - 4 - 150, this.height / 4 + 30 + 12, 150, 20, CommonComponents.GUI_DONE, (p_97691_) -> {
            this.onDone();
        }));
        this.cancelButton = this.addRenderableWidget(new Button(this.width / 2 + 4, this.height / 4 + 30 + 12, 150, 20, CommonComponents.GUI_CANCEL, (p_97687_) -> {
            this.onClose();
        }));
        // Create a message box in the center of the screen
        this.messageBox = new EditBox(this.font, this.width / 2 - 150, 50, 300, 20, new TranslatableComponent(SwarmsmpS2.translationKey + "gui.sending_stone.message", 25));
        this.messageBox.setMaxLength(25);
        this.messageBox.setVisible(true);
        this.addRenderableWidget(this.messageBox);
        this.setInitialFocus(this.messageBox);
        this.messageBox.setFocus(true);
    }

    private void onDone() {
        this.minecraft.setScreen(null);
        String message = this.messageBox.getValue();
        // We'll send a packet to the server with our message, sender, and the UUID of the item
        // The server will then send a message to any player with a stone with the same UUID
        if (this.minecraft.player == null) {
            return;
        }
        UUID playerUUID = this.minecraft.player.getUUID();
        UUID itemUUID = UUID.fromString(this.item.getOrCreateTag().getString(SwarmsmpS2.MODID + ":paired"));
        ItemStack heldItem = this.minecraft.player.getMainHandItem();
        SwarmSMPPacketHandler.SENDING_STONE_CHANNEL.sendToServer(new SendingStonePacket(playerUUID, message, itemUUID.toString(), heldItem));
        LOGGER.info("Sending message from " + playerUUID + " with UUID " + itemUUID + ": " + message);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String s = this.messageBox.getValue();
        this.init(minecraft, width, height);
        this.messageBox.setValue(s);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        return this.messageBox.isFocused() ? this.messageBox.keyPressed(keyCode, scanCode, modifiers) : super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, this.font, TITLE, this.width / 2, 20, 16777215);
        drawString(poseStack, this.font, LABEL, this.width / 2 - 150, 37, 10526880);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
