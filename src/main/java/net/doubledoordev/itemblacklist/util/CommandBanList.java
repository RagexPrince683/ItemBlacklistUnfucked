package net.doubledoordev.itemblacklist.util;

import net.doubledoordev.itemblacklist.ItemBlacklist;
import net.doubledoordev.itemblacklist.data.GlobalBanList;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Public, read-only view of the world and pack ban lists. */
public class CommandBanList extends CommandBase
{
    @Override
    public String getCommandName()
    {
        return "banlist";
    }

    @Override
    public List getCommandAliases()
    {
        return Arrays.asList("itembanlist");
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    //didn't add the fucking boolean to let non ops use it award
    public boolean canCommandSenderUseCommand(ICommandSender sender)
    {
        return true;
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/banlist [dimension|__GLOBAL__]";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        boolean administrator = !(sender instanceof EntityPlayer)
                || sender.canCommandSenderUseCommand(2, "itemblacklist");
        if (!ItemBlacklist.publicBanListEnabled && !administrator)
        {
            sender.addChatMessage(new ChatComponentText("The public banned item list is disabled."));
            return;
        }
        if (args.length > 1) throw new WrongUsageException(getCommandUsage(sender));
        if (args.length == 0) BanListDisplay.displayAll(sender);
        else if (GlobalBanList.GLOBAL_NAME.equalsIgnoreCase(args[0])) BanListDisplay.displayGlobal(sender);
        else
        {
            try
            {
                BanListDisplay.displayDimension(sender, Integer.parseInt(args[0]));
            }
            catch (NumberFormatException e)
            {
                throw new WrongUsageException(getCommandUsage(sender));
            }
        }
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (args.length != 1) return null;
        Set<String> dimensions = new HashSet<>();
        dimensions.add(GlobalBanList.GLOBAL_NAME);
        addDimensions(dimensions, GlobalBanList.worldInstance);
        addDimensions(dimensions, GlobalBanList.packInstance);
        return getListOfStringsFromIterableMatchingLastWord(args, dimensions);
    }

    private void addDimensions(Set<String> dimensions, GlobalBanList source)
    {
        if (source == null) return;
        source.dimesionMap.values().forEach(list -> dimensions.add(list.getDimension()));
    }
}
