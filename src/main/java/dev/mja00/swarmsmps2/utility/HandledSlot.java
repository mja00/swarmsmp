package dev.mja00.swarmsmps2.utility;

import net.minecraft.world.inventory.AbstractContainerMenu;

public interface HandledSlot {

    AbstractContainerMenu getContainerMenu();
    void setContainerMenu(AbstractContainerMenu menu);
}
