package dev.mja00.swarmsmps2.mixin;

import dev.mja00.swarmsmps2.utility.CompoundContainerHelper;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CompoundContainer.class)
public abstract class CompoundContainerMixin implements CompoundContainerHelper {
    @Shadow
    @Final
    private Container container1;
    @Shadow
    @Final
    private Container container2;

    @NotNull
    @Override
    public Container getContainer(int slot) {
        return slot >= this.container1.getContainerSize() ? this.container2 : this.container1;
    }
}
