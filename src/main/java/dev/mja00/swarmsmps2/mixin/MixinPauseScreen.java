package dev.mja00.swarmsmps2.mixin;

import com.mojang.realmsclient.RealmsMainScreen;
import dev.mja00.swarmsmps2.SSMPS2Config;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class MixinPauseScreen extends Screen {

    protected MixinPauseScreen(Component pTitle) {
        super(pTitle);
    }

    // We wanna do our own little pause menu :)
    @Inject(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/PauseScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;", ordinal = 7), cancellable = true)
    private void swarmsmp_s2$createPauseMenu(CallbackInfo ci) {
        if (SSMPS2Config.CLIENT.saoMode.get()) {
            Component component = new TextComponent("HAHAHAHAHAHAHAHAHAHA");
            Button button = this.addRenderableWidget(new Button(this.width / 2 - 102, this.height / 4 + 120 + -16, 204, 20, component, (p_96315_) -> {
                boolean flag = this.minecraft.isLocalServer();
                boolean flag1 = this.minecraft.isConnectedToRealms();
                p_96315_.active = false;
                this.minecraft.level.disconnect();
                if (flag) {
                    this.minecraft.clearLevel(new GenericDirtMessageScreen(new TranslatableComponent("menu.savingLevel")));
                } else {
                    this.minecraft.clearLevel();
                }

                TitleScreen titlescreen = new TitleScreen();
                if (flag) {
                    this.minecraft.setScreen(titlescreen);
                } else if (flag1) {
                    this.minecraft.setScreen(new RealmsMainScreen(titlescreen));
                } else {
                    this.minecraft.setScreen(new JoinMultiplayerScreen(titlescreen));
                }

            }));
            button.active = false;
            // We'll also toggle the saoMode here which'll make it disable next menu opening
            SSMPS2Config.CLIENT.saoMode.set(false);
            // Cancelling here should no longer render the disconnect button
            ci.cancel();
        }
    }

}
