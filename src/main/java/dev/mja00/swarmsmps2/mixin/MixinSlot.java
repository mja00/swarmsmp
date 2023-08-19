package dev.mja00.swarmsmps2.mixin;

import dev.mja00.swarmsmps2.SwarmsmpS2;
import dev.mja00.swarmsmps2.objects.BlockEventObject;
import dev.mja00.swarmsmps2.utility.CompoundContainerHelper;
import dev.mja00.swarmsmps2.utility.HandledSlot;
import dev.mja00.swarmsmps2.utility.HandlerWithPlayer;
import dev.mja00.swarmsmps2.utility.LocationalContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(Slot.class)
public abstract class MixinSlot implements HandledSlot {
    private ItemStack oldStack = null;
    private AbstractContainerMenu handler = null;
    private static final Logger LOGGER = LogManager.getLogger("SSMPS2/MixinSlot");


    @Shadow
    @Final
    public Container container;
    @Shadow
    public int index;

    @Shadow public abstract ItemStack getItem();

    @NotNull
    @Override
    public AbstractContainerMenu getContainerMenu() {
        return handler;
    }

    @Override
    public void setContainerMenu(AbstractContainerMenu handler) {
        this.handler = handler;
        oldStack = this.getItem() == null ? ItemStack.EMPTY : this.getItem().copy();
    }

    @Inject(method = "setChanged", at = @At("HEAD"))
    public void swarmsmp_s2$set(CallbackInfo ci) {
        BlockPos pos = getContainerLocation();
        HandlerWithPlayer handlerWithPlayer = (HandlerWithPlayer) handler;
        if (pos != null && handlerWithPlayer.getPlayer() != null) {
            // Print something pretty :)
            logChange(handlerWithPlayer.getPlayer(), oldStack, this.getItem().copy(), pos);
        }
        oldStack = this.getItem().copy();
    }
    
    @Unique
    @Nullable
    private BlockPos getContainerLocation() {
        Container slotContainer = this.container;
        if (slotContainer instanceof CompoundContainerHelper) {
            slotContainer = ((CompoundContainerHelper) slotContainer).getContainer(this.index);
        }
        if (slotContainer instanceof LocationalContainer) {
            return ((LocationalContainer) slotContainer).getLocation();
        }

        return null;
    }

    @Unique
    private void logChange(ServerPlayer player, ItemStack stack, ItemStack newStack, BlockPos pos) {
        // If both stacks are empty ignore
        if (stack.isEmpty() && newStack.isEmpty()) {
            return;
        }

        // 2 non-empty stacks
        if (!stack.isEmpty() && !newStack.isEmpty()) {
            if (stack.getItem() == newStack.getItem()) {
                // Add or remove stacks of the same type
                int newCount = newStack.getCount();
                int oldCount = stack.getCount();
                if (newCount > oldCount) { // Add items
                    logChange(player, ItemStack.EMPTY, new ItemStack(newStack.getItem(), newCount - oldCount), pos);
                } else { // Removed items
                    logChange(player, new ItemStack(newStack.getItem(), oldCount - newCount), ItemStack.EMPTY, pos);
                }
            } else { // Split the actions
                logChange(player, stack, ItemStack.EMPTY, pos); // Log taking out old stack
                logChange(player, ItemStack.EMPTY, newStack, pos); // Log putting in new stack
            }
            return;
        }

        boolean oldEmpty = stack.isEmpty(); // One empty
        ItemStack changedStack = oldEmpty ? newStack : stack;
        BlockEventObject bEvent = new BlockEventObject(player.getStringUUID(), changedStack.getItem().getRegistryName().toString(), oldEmpty ? "item_add" : "item_remove", pos.getX(), pos.getY(), pos.getZ(), 0, changedStack.getCount());

        if (SwarmsmpS2.sqlite == null) { return; }
        SwarmsmpS2.sqlite.createWorldEvent(bEvent);
    }
}
