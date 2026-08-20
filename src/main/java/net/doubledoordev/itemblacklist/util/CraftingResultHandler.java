package net.doubledoordev.itemblacklist.util;

import net.doubledoordev.itemblacklist.Helper;
import net.doubledoordev.itemblacklist.ItemBlacklist;
import net.doubledoordev.itemblacklist.data.GlobalBanList;
import net.doubledoordev.itemblacklist.data.SpecialRuleList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.SlotCrafting;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.util.List;
import java.lang.reflect.Field;
import cpw.mods.fml.relauncher.ReflectionHelper;

/**
 * Keeps banned crafting outputs packed and decides whether a container click
 * must be stopped before vanilla can handle it.
 */
public final class CraftingResultHandler
{
    // SlotCrafting.craftMatrix / field_75239_a in MCP/SRG 1.7.10.
    private static final Field CRAFT_MATRIX_FIELD = ReflectionHelper.findField(
            SlotCrafting.class, "craftMatrix", "field_75239_a");

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
            if (stack == null) continue;
            boolean packed = stack.getItem() == ItemBlacklisted.I && ItemBlacklisted.canUnpack(stack);
            ItemStack realResult = packed ? ItemBlacklisted.unpack(stack) : stack;
            if (realResult == stack && packed) continue;
            boolean banned = GlobalBanList.isBanned(player.dimension, realResult);
            boolean allowed = SpecialRuleList.craftAllow.matches(player.dimension, realResult);
            ItemStack replacement = banned && !allowed
                    ? (packed ? stack : ItemBlacklisted.pack(stack))
                    : (packed ? realResult : stack);
            if (replacement != stack) slot.putStack(replacement);
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

        if (containsCraftingOnlyIngredient((SlotCrafting) slot, player.dimension))
        {
            player.addChatComponentMessage(new ChatComponentText(
                    "One or more items in this recipe cannot be used for crafting."));
            return true;
        }

        ItemStack realResult = packed ? ItemBlacklisted.unpack(stack) : stack;
        if (packed && realResult != stack && SpecialRuleList.craftAllow.matches(player.dimension, realResult))
        {
            slot.putStack(realResult);
            container.detectAndSendChanges();
            return false;
        }

        if (!packed && GlobalBanList.isBanned(player.dimension, stack)
                && !SpecialRuleList.craftAllow.matches(player.dimension, stack))
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

    private static boolean containsCraftingOnlyIngredient(SlotCrafting slot, int dimension)
    {
        final InventoryCrafting matrix;
        try
        {
            matrix = (InventoryCrafting) CRAFT_MATRIX_FIELD.get(slot);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("Could not read SlotCrafting crafting matrix.", e);
        }
        for (int index = 0; index < matrix.getSizeInventory(); index++)
        {
            ItemStack ingredient = matrix.getStackInSlot(index);
            if (ingredient != null && SpecialRuleList.craftingOnly.matches(dimension, ingredient)) return true;
        }
        return false;
    }
}
