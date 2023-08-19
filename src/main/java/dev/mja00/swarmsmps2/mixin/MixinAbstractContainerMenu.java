package dev.mja00.swarmsmps2.mixin;

import dev.mja00.swarmsmps2.utility.HandledSlot;
import dev.mja00.swarmsmps2.utility.HandlerWithPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(AbstractContainerMenu.class)
public abstract class MixinAbstractContainerMenu implements HandlerWithPlayer {
    @Unique
    private ServerPlayer player = null;

    @Inject(method = "addSlot", at = @At(value = "HEAD"))
    private void giveContainerMenuReference(Slot slot, CallbackInfoReturnable<Slot> cir) {
        ((HandledSlot) slot).setContainerMenu((AbstractContainerMenu) (Object) this);
    }

    @Inject(method = "doClick", at = @At(value = "HEAD"))
    private void doClickGetPlayer(int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_, CallbackInfo ci) {
        this.player = (ServerPlayer) p_150434_;
    }

    @Inject(method = "clicked", at = @At(value = "HEAD"))
    private void clickedGetPlayer(int p_75144_1_, int p_75144_2_, ClickType p_75144_3_, Player p_75144_4_, CallbackInfo ci) {
        this.player = (ServerPlayer) p_75144_4_;
    }

    @Inject(method = "clickMenuButton", at = @At(value = "HEAD"))
    private void clickMenuButtonGetPlayer(Player pPlayer, int pId, CallbackInfoReturnable<Boolean> cir) {
        this.player = (ServerPlayer) pPlayer;
    }

    @Inject(method = "clearContainer", at = @At(value = "HEAD"))
    private void clearContainerGetPlayer(Player p_150412_, Container p_150413_, CallbackInfo ci) {
        this.player = (ServerPlayer) p_150412_;
    }

    @Nullable
    @Override
    public ServerPlayer getPlayer() {
        return player;
    }
}
