package dev.mja00.swarmsmps2.item.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MailItem extends Item {
    public MailItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack envelope = pPlayer.getItemInHand(pUsedHand);
        // Create a new single itemstack of the envelope for our target
        ItemStack targetEnvelope = new ItemStack(this);
        // If it's torn do nothing
        if (isTorn(envelope)) return InteractionResultHolder.pass(envelope);
        if (isOpen(envelope)) {
            CompoundTag tags = targetEnvelope.getOrCreateTag();
            // Get the opposite hand
            InteractionHand oppositeHand = pUsedHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack offhand = pPlayer.getItemInHand(oppositeHand);
            // If the offhand is empty, do nothing
            if (offhand.isEmpty()) return InteractionResultHolder.pass(envelope);
            // We basically want to "put" the item in the offhand into the envelope
            // We'll do this by storing the item in the envelope's NBT, it needs to be an exact copy
            CompoundTag offhandItem = offhand.save(new CompoundTag());
            tags.put("swarmsmps2.item", offhandItem);
            // We'll seal it
            tags.putString("swarmsmps2.state", "sealed");
            tags.putString("swarmsmps2.sender", pPlayer.getName().getString());
            // We'll take the item out of the offhand
            pPlayer.setItemInHand(oppositeHand, ItemStack.EMPTY);
            // Add the item to the player's inventory
            targetEnvelope.setCount(1);
            pPlayer.addItem(targetEnvelope);
           envelope.shrink(1);
        } else if (isSealed(envelope)) {
            CompoundTag tags = envelope.getOrCreateTag();
            // We'll open it
            tags.putString("swarmsmps2.state", "torn");
            tags.putString("swarmsmps2.opener", pPlayer.getName().getString());
            // Get the item from the envelope's NBT
            CompoundTag item = tags.getCompound("swarmsmps2.item");
            // Create an itemstack from the NBT
            ItemStack itemStack = ItemStack.of(item);
            // Give the item to the player
            pPlayer.addItem(itemStack);
            // Clear the item from the envelope's NBT, this'll let them stack
            tags.remove("swarmsmps2.item");
            tags.remove("swarmsmps2.sender");
            return InteractionResultHolder.success(envelope);
        }
        // it should never get here
        return InteractionResultHolder.pass(envelope);
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        // If it's the torn variant, it's fuel
        if (isTorn(itemStack)) return 100;
        return super.getBurnTime(itemStack, recipeType);
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        // If it's sealed it's foil
        return getMailType(pStack).itemModelIndex == 0.20f;
    }

    private boolean isTorn(ItemStack pStack) {
        // If it's torn it's torn
        return getMailType(pStack).itemModelIndex == 0.30f;
    }

    private boolean isSealed(ItemStack pStack) {
        // If it's sealed it's sealed
        return getMailType(pStack).itemModelIndex == 0.20f;
    }

    private boolean isOpen(ItemStack pStack) {
        // If it's open it's open
        return getMailType(pStack).itemModelIndex == 0.10f;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        // Get the current state of the mail
        float mailType = getMailType(pStack).itemModelIndex;
        // Add the tooltip
        if (mailType == 0.10f) {
            pTooltipComponents.add(Component.nullToEmpty("Open"));
        } else if (mailType == 0.20f) {
            pTooltipComponents.add(Component.nullToEmpty("Sealed"));
            // Add the "Sealed by <player>" tooltip
            CompoundTag tags = pStack.getTag();
            if (tags != null) {
                String sender = tags.getString("swarmsmps2.sender");
                if (!sender.isEmpty()) {
                    pTooltipComponents.add(Component.nullToEmpty("Sealed by " + sender));
                }
            }
        } else if (mailType == 0.30f) {
            pTooltipComponents.add(Component.nullToEmpty("Torn"));
        }
    }

    public static float getItemTextureIndex(ItemStack stack) {
        var type = getMailType(stack);
        if (type != null) return type.itemModelIndex;
        return 0f;
    }

    public static SpecialType getMailType(ItemStack stack) {
        // We want to read the NBT tag and check its state
        CompoundTag tags = stack.getTag();
        if (tags == null) {
            // If there's no tag, it's open
            return MAIL_TYPES.get("open");
        }
        // Now we check the "state" tag
        String state = tags.getString("swarmsmps2.state");
        return MAIL_TYPES.getOrDefault(state, new SpecialType(0.10f));
    }

    private static final Map<String, SpecialType> MAIL_TYPES = new HashMap<>() {{
        put("open", new SpecialType(0.10f));
        put("sealed", new SpecialType(0.20f));
        put("torn", new SpecialType(0.30f));
    }};

    public record SpecialType(float itemModelIndex) {}

}
