package dev.mja00.swarmsmps2.utility;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DeadPlayerInventory implements Container {
    private final List<ItemStack> items;

    public DeadPlayerInventory(List<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return 45;
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isInvalidSlot(int index) {
        return index >= 4 && index < 8;
    }

    public int getSlot(int index) {
        if (index == 8) {
            return 40;
        } else if (index >= 0 && index <= 3) {
            return 39 - index;
        } else if (index >= 9 && index <= 35) {
            return index;
        } else if (index >= 36 && index <= 44) {
            return index - 36;
        }

        return -1;
    }

    @Override
    public ItemStack getItem(int index) {
        if (isInvalidSlot(index)) {
            return ItemStack.EMPTY;
        }

        int slot = getSlot(index);
        return slot == -1 ? ItemStack.EMPTY : items.get(slot);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        int slot = getSlot(index);
        return slot == -1 ? ItemStack.EMPTY : items.get(slot).copy();
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        int slot = getSlot(index);
        return slot == -1 ? ItemStack.EMPTY : items.get(slot).copy();
    }

    @Override
    public void setItem(int index, ItemStack is) {
        return;
    }

    @Override
    public void setChanged() {
        return;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return false;
    }

    @Override
    public void clearContent() {
        return;
    }
}
