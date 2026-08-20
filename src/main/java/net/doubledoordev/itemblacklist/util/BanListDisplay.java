package net.doubledoordev.itemblacklist.util;

import net.doubledoordev.itemblacklist.data.BanList;
import net.doubledoordev.itemblacklist.data.BanListEntry;
import net.doubledoordev.itemblacklist.data.GlobalBanList;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static net.minecraft.util.EnumChatFormatting.AQUA;
import static net.minecraft.util.EnumChatFormatting.RED;
import static net.minecraft.util.EnumChatFormatting.YELLOW;

/** Shared, read-only rendering for the administrative and public list commands. */
public final class BanListDisplay
{
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm a z", Locale.US);

    private BanListDisplay()
    {
    }

    public static void displayAll(ICommandSender sender)
    {
        display(sender, collectAll(GlobalBanList.worldInstance), collectAll(GlobalBanList.packInstance));
    }

    public static void displayGlobal(ICommandSender sender)
    {
        Set<BanList> world = new HashSet<>();
        world.add(GlobalBanList.worldInstance.getGlobal());
        Set<BanList> pack = new HashSet<>();
        if (GlobalBanList.packInstance != null) pack.add(GlobalBanList.packInstance.getGlobal());
        display(sender, world, pack);
    }

    public static void displayDimension(ICommandSender sender, int dimension)
    {
        Set<BanList> world = new HashSet<>(GlobalBanList.worldInstance.dimesionMap.get(dimension));
        Set<BanList> pack = GlobalBanList.packInstance == null ? new HashSet<BanList>()
                : new HashSet<>(GlobalBanList.packInstance.dimesionMap.get(dimension));
        display(sender, world, pack);
    }

    private static Set<BanList> collectAll(GlobalBanList source)
    {
        Set<BanList> result = new HashSet<>();
        if (source != null)
        {
            result.addAll(source.dimesionMap.values());
            result.add(source.getGlobal());
        }
        return result;
    }

    private static void display(ICommandSender sender, Set<BanList> world, Set<BanList> pack)
    {
        displaySection(sender, "World banned items:", "No world banned items.", world, false);
        displaySection(sender, "Pack banned items: ", "No pack banned items. ", pack, true);
    }

    private static void displaySection(ICommandSender sender, String heading, String empty,
            Set<BanList> lists, boolean unchangeable)
    {
        ChatComponentText message = new ChatComponentText(lists.isEmpty() ? empty : heading);
        message.setChatStyle(new ChatStyle().setColor(YELLOW));
        if (unchangeable)
            message.appendSibling(new ChatComponentText("[unchangeable]").setChatStyle(new ChatStyle().setColor(RED)));
        sender.addChatMessage(message);
        for (BanList list : lists)
        {
            sender.addChatMessage(new ChatComponentText("Dimension " + list.getDimension())
                    .setChatStyle(new ChatStyle().setColor(AQUA)));
            for (BanListEntry entry : list.banListEntryMap.values())
            {
                String expiration = entry.isPermanent() ? "" : " [unbans "
                        + DISPLAY_TIME.format(entry.getExpiresAt().atZone(ZoneId.systemDefault())) + "]";
                sender.addChatMessage(new ChatComponentText(entry.toString() + expiration));
            }
        }
    }
}
