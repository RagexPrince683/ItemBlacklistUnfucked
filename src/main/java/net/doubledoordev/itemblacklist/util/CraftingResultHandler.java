package net.doubledoordev.itemblacklist.util;

import net.doubledoordev.itemblacklist.Helper;
import net.doubledoordev.itemblacklist.ItemBlacklist;
import net.doubledoordev.itemblacklist.data.GlobalBanList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.util.List;

/**
 * Keeps banned crafting outputs packed and decides whether a container click
 * must be stopped before vanilla can handle it.
 */
public final class CraftingResultHandler
{
    private CraftingResultHandler()
    {
    }

    /**
     * Updates only crafting result slots, rather than reprocessing the entire
     * open container on every PlayerOpenContainerEvent.
     */
    public static void updateCraftingResults(EntityPlayer player, Container container)
    {
        if (player == null || container == null) return;

        //noinspection unchecked
        for (Slot slot : (List<Slot>) container.inventorySlots)
        {
            if (!(slot instanceof SlotCrafting)) continue;

            ItemStack stack = slot.getStack();
            ItemStack processed = GlobalBanList.process(player.dimension, stack);
            if (processed != stack) slot.putStack(processed);
        }
    }

    /**
     * Called at the head of Container.slotClick on both physical sides.
     * The server checks an unpacked result against its authoritative ban list;
     * the client only recognizes the packed marker sent by the server.
     */
    public static boolean isBlockedCraftingResult(EntityPlayer player, Container container, int slotId)
    {
        if (player == null || container == null || slotId < 0 || slotId >= container.inventorySlots.size()) return false;

        Slot slot = (Slot) container.inventorySlots.get(slotId);
        if (!(slot instanceof SlotCrafting)) return false;

        ItemStack stack = slot.getStack();
        if (stack == null) return false;

        boolean packed = stack.getItem() == ItemBlacklisted.I && ItemBlacklisted.canUnpack(stack);
        if (player.worldObj.isRemote) return packed;
        if (!Helper.shouldCare(player)) return false;

        if (!packed && GlobalBanList.isBanned(player.dimension, stack))
        {
            // Close the recipe-update/tick race before rejecting the click and
            // immediately make the authoritative visible result the marker.
            slot.putStack(ItemBlacklisted.pack(stack));
            container.detectAndSendChanges();
            packed = true;
        }

        if (packed) player.addChatComponentMessage(new ChatComponentText(ItemBlacklist.message));
        return packed;
    }
}
