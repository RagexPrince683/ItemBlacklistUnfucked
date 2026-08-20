package net.doubledoordev.itemblacklist.util;

import cpw.mods.fml.common.registry.GameRegistry;
import net.doubledoordev.itemblacklist.Helper;
import net.doubledoordev.itemblacklist.ItemBlacklist;
import net.doubledoordev.itemblacklist.data.BanListEntry;
import net.doubledoordev.itemblacklist.data.GlobalBanList;
import net.doubledoordev.itemblacklist.data.SpecialRuleList;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.util.EnumChatFormatting.*;

/**
 * @author Dries007
 */
public class CommandBlockItem extends CommandBase
{
    private static final Pattern DURATION_PART = Pattern.compile("(\\d+)([ydhms])", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATE = Pattern.compile("(\\d{2})-(\\d{2})-(\\d{4})");
    private static final Pattern TIME = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?([ap]m)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("MM-dd-yyyy hh:mm a z", Locale.US);

    private static class Schedule
    {
        private final Instant expiresAt;
        private final String duration;
        private Schedule(Instant expiresAt, String duration)
        {
            this.expiresAt = expiresAt;
            this.duration = duration;
        }
    }
    public static class Pair<K, V>
    {
        public K k;
        public V v;

        public Pair(K k, V v)
        {
            this.k = k;
            this.v = v;
        }
    }

    @Override
    public String getCommandName()
    {
        return "blockitem";
    }

    @Override
    public List getCommandAliases()
    {
        return Arrays.asList("itemblacklist", "blacklist");
    }

    @Override
    public String getCommandUsage(ICommandSender p_71518_1_)
    {
        return "Use '/blockitem help' for more info.";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args)
    {
        if (args.length == 0)
        {
            sender.addChatMessage(new ChatComponentText("Possible subcommands:").setChatStyle(new ChatStyle().setColor(GOLD)));
            sender.addChatMessage(makeHelpText("reload", "Reloads the config file from disk."));
            sender.addChatMessage(makeHelpText("pack [player]", "Lock banned items in targets inventory."));
            sender.addChatMessage(makeHelpText("unpack [player]", "Unlock banned items in targets inventory."));
            sender.addChatMessage(makeHelpText("list [dim|player]", "List banned items of all, player, or dim"));
            sender.addChatMessage(makeHelpText("publiclist [on|off]", "Controls non-op access to /banlist."));
            sender.addChatMessage(makeHelpText("ban [dim] [item] [timer <duration>|date <MM-dd-yyyy> <time>]", "Ban wildcard metadata; no schedule is permanent."));
            sender.addChatMessage(makeHelpText("meta [dim] [timer <duration>|date <MM-dd-yyyy> <time>]", "Ban the exact held metadata; no schedule is permanent."));
            sender.addChatMessage(makeHelpText("timers", "y years, d days, h hours, m minutes, s seconds (example: 1d12h)."));
            sender.addChatMessage(makeHelpText("unban [dim list] [item[:*|meta]]", "Unban an item."));
            sender.addChatMessage(makeHelpText("unmeta [dim list]", "Unban only the held metadata variant."));
            sender.addChatMessage(makeHelpText("allowbanneditemcraft [dim] [item]", "Allow a normally banned item to be produced by crafting."));
            sender.addChatMessage(makeHelpText("disallowbanneditemcraft [dim] [item]", "Remove a banned-item crafting exception."));
            sender.addChatMessage(makeHelpText("banplacementonly [dim] [item]", "Prevent block placement only."));
            sender.addChatMessage(makeHelpText("unbanplacementonly [dim] [item]", "Remove a placement-only ban."));
            sender.addChatMessage(makeHelpText("bancraftingonly [dim] [item]", "Prevent use as a crafting ingredient only."));
            sender.addChatMessage(makeHelpText("unbancraftingonly [dim] [item]", "Remove a crafting-only ban."));
            return;
        }
        String arg0 = args[0].toLowerCase();
        boolean unpack = false;
        switch (arg0)
        {
            default:
                throw new WrongUsageException("Unknown subcommand. Use '/blockitem' to get some help.");
            case "reload":
                GlobalBanList.init();
                ServerEventHandlers.refreshOnlinePlayers();
                // No break
                sender.addChatMessage(new ChatComponentText("Reloaded!").setChatStyle(new ChatStyle().setColor(GREEN)));
            case "list":
                list(sender, args);
                break;
            case "publiclist":
                publicList(sender, args);
                break;
            case "unpack":
                unpack = true;
            case "pack":
                EntityPlayer player = args.length > 1 ? getPlayer(sender, args[1]) : getCommandSenderAsPlayer(sender);
                int count = GlobalBanList.process(player.dimension, player.inventory, unpack);
                sender.addChatMessage(new ChatComponentText((unpack ? "Unlocked " : "Locked ") + count + " items."));
                break;
            case "ban":
                try
                {
                    String[] banArgs = args.clone();
                    Schedule schedule = parseSchedule(banArgs);
                    banArgs = stripSchedule(banArgs, schedule);
                    Pair<String, BanListEntry> toBan = parse(sender, banArgs);
                    toBan.v.setExpiresAt(schedule == null ? null : schedule.expiresAt);
                    GlobalBanList.worldInstance.add(toBan.k, toBan.v);
                    sendBanFeedback(sender, toBan, schedule);
                }
                catch (Exception e)
                {
                    if (e instanceof CommandException) throw (CommandException) e;
                    throw new WrongUsageException(e.getMessage());
                }
                break;
            case "meta":
                changeHeldMetaBan(sender, args, false, true);
                break;
            case "unmeta":
                changeHeldMetaBan(sender, args, true, false);
                break;
            case "unban":
                try
                {
                    Pair<String, BanListEntry> toBan = parse(sender, args);
                    if (GlobalBanList.worldInstance.remove(toBan.k, toBan.v)) sender.addChatMessage(new ChatComponentText("Unbanned " + toBan.v.toString() + " in " + toBan.k).setChatStyle(new ChatStyle().setColor(GREEN)));
                    else sender.addChatMessage(new ChatComponentText("Can't unban " + toBan.v.toString() + " in " + toBan.k).setChatStyle(new ChatStyle().setColor(RED)));
                }
                catch (Exception e)
                {
                    if (e instanceof CommandException) throw (CommandException) e;
                    throw new WrongUsageException(e.getMessage());
                }
                break;
            case "allowbanneditemcraft":
                changeSpecialRule(sender, args, SpecialRuleList.craftAllow, false, "Added crafting exception for ");
                break;
            case "disallowbanneditemcraft":
                changeSpecialRule(sender, args, SpecialRuleList.craftAllow, true, "Removed crafting exception for ");
                break;
            case "banplacementonly":
                changeSpecialRule(sender, args, SpecialRuleList.placementOnly, false, "Added placement-only ban for ");
                break;
            case "unbanplacementonly":
                changeSpecialRule(sender, args, SpecialRuleList.placementOnly, true, "Removed placement-only ban for ");
                break;
            case "bancraftingonly":
                changeSpecialRule(sender, args, SpecialRuleList.craftingOnly, false, "Added crafting-only ban for ");
                break;
            case "unbancraftingonly":
                changeSpecialRule(sender, args, SpecialRuleList.craftingOnly, true, "Removed crafting-only ban for ");
                break;
        }
    }

    private void changeSpecialRule(ICommandSender sender, String[] args, SpecialRuleList rules,
            boolean remove, String success)
    {
        try
        {
            Pair<String, BanListEntry> parsed = parseSpecial(sender, args);
            boolean changed;
            if (remove) changed = rules.remove(parsed.k, parsed.v);
            else
            {
                rules.add(parsed.k, parsed.v);
                changed = true;
            }
            if (changed)
            {
                sender.addChatMessage(new ChatComponentText(success + parsed.v + " in " + parsed.k + ".")
                        .setChatStyle(new ChatStyle().setColor(GREEN)));
                ServerEventHandlers.refreshOnlinePlayers();
            }
            else sender.addChatMessage(new ChatComponentText("No matching special rule for " + parsed.v + " in " + parsed.k + ".")
                    .setChatStyle(new ChatStyle().setColor(RED)));
        }
        catch (Exception e)
        {
            if (e instanceof CommandException) throw (CommandException) e;
            throw new WrongUsageException(e.getMessage());
        }
    }

    private Pair<String, BanListEntry> parseSpecial(ICommandSender sender, String[] args)
    {
        String dimensions = null;
        BanListEntry entry = null;
        for (int i = 1; i < args.length; i++)
        {
            String argument = args[i];
            if (GlobalBanList.GLOBAL_NAME.equalsIgnoreCase(argument))
            {
                if (dimensions != null) throw new WrongUsageException("Double dimension specifiers.");
                dimensions = GlobalBanList.GLOBAL_NAME;
                continue;
            }
            try
            {
                Helper.parseDimIds(argument);
                if (dimensions != null) throw new WrongUsageException("Double dimension specifiers.");
                dimensions = argument;
                continue;
            }
            catch (WrongUsageException e) { throw e; }
            catch (Exception ignored) { }

            String[] item = argument.split(":", -1);
            if (item.length < 2 || item.length > 3 || item[0].isEmpty() || item[1].isEmpty())
                throw new WrongUsageException("Not a valid registry item: " + argument);
            if (entry != null) throw new WrongUsageException("Double item specifiers.");
            int meta = OreDictionary.WILDCARD_VALUE;
            if (item.length == 3 && !"*".equals(item[2])) meta = parseInt(sender, item[2]);
            entry = new BanListEntry(item[0] + ":" + item[1], meta);
        }
        EntityPlayer player = null;
        if (dimensions == null || entry == null) player = getCommandSenderAsPlayer(sender);
        if (dimensions == null) dimensions = String.valueOf(player.dimension);
        if (entry == null)
        {
            ItemStack held = player.getHeldItem();
            if (held == null) throw new WrongUsageException("No item specified and no item held.");
            entry = new BanListEntry(GameRegistry.findUniqueIdentifierFor(held.getItem()), held.getItemDamage());
        }
        return new Pair<>(dimensions, entry);
    }

    private void publicList(ICommandSender sender, String[] args)
    {
        if (args.length > 2) throw new WrongUsageException("/itemblacklist publiclist [on|off]");
        if (args.length == 2)
        {
            boolean enabled;
            if ("on".equalsIgnoreCase(args[1])) enabled = true;
            else if ("off".equalsIgnoreCase(args[1])) enabled = false;
            else throw new WrongUsageException("/itemblacklist publiclist [on|off]");
            ItemBlacklist.setPublicBanListEnabled(enabled);
            ItemBlacklist.logger.info("{} {} public ban list access.", sender.getCommandSenderName(),
                    enabled ? "enabled" : "disabled");
        }
        sender.addChatMessage(new ChatComponentText("Public ban list access is "
                + (ItemBlacklist.publicBanListEnabled ? "enabled." : "disabled."))
                .setChatStyle(new ChatStyle().setColor(GREEN)));
    }

    private void changeHeldMetaBan(ICommandSender sender, String[] args, boolean remove, boolean allowSchedule)
    {
        try
        {
            Schedule schedule = allowSchedule ? parseSchedule(args) : null;
            String[] parsedArgs = stripSchedule(args, schedule);
            Pair<String, BanListEntry> entry = parseHeldMeta(sender, parsedArgs);
            if (!remove)
            {
                entry.v.setExpiresAt(schedule == null ? null : schedule.expiresAt);
                GlobalBanList.worldInstance.add(entry.k, entry.v);
                sendBanFeedback(sender, entry, schedule);
            }
            else if (GlobalBanList.worldInstance.remove(entry.k, entry.v))
            {
                sender.addChatMessage(new ChatComponentText("Unbanned " + entry.v + " in " + entry.k).setChatStyle(new ChatStyle().setColor(GREEN)));
            }
            else
            {
                sender.addChatMessage(new ChatComponentText("Can't unban " + entry.v + " in " + entry.k).setChatStyle(new ChatStyle().setColor(RED)));
            }
        }
        catch (Exception e)
        {
            if (e instanceof CommandException) throw (CommandException) e;
            throw new WrongUsageException(e.getMessage());
        }
    }

    private void sendBanFeedback(ICommandSender sender, Pair<String, BanListEntry> entry, Schedule schedule)
    {
        String suffix = schedule != null && schedule.duration != null ? " for " + schedule.duration : "";
        sender.addChatMessage(new ChatComponentText("Banned " + entry.v + " in " + entry.k + suffix + ".")
                .setChatStyle(new ChatStyle().setColor(GREEN)));
        if (schedule != null)
        {
            String local = DISPLAY_TIME.format(schedule.expiresAt.atZone(ZoneId.systemDefault()));
            sender.addChatMessage(new ChatComponentText("Unban scheduled for " + local + ".")
                    .setChatStyle(new ChatStyle().setColor(GREEN)));
        }
    }

    private String[] stripSchedule(String[] args, Schedule schedule)
    {
        if (schedule == null) return args;
        int count = args[args.length - 2].equalsIgnoreCase("timer") ? 2 : 3;
        return Arrays.copyOf(args, args.length - count);
    }

    private Schedule parseSchedule(String[] args)
    {
        List<Integer> keywords = new ArrayList<>();
        for (int i = 1; i < args.length; i++)
            if (args[i].equalsIgnoreCase("timer") || args[i].equalsIgnoreCase("date")) keywords.add(i);
        if (keywords.isEmpty()) return null;
        if (keywords.size() > 1)
        {
            boolean timer = false, date = false;
            for (Integer index : keywords)
            {
                timer |= args[index].equalsIgnoreCase("timer");
                date |= args[index].equalsIgnoreCase("date");
            }
            throw new WrongUsageException(timer && date ? "Cannot use both timer and date schedules." : "Only one schedule may be specified.");
        }
        int index = keywords.get(0);
        if (args[index].equalsIgnoreCase("timer"))
        {
            if (index + 1 >= args.length) throw new WrongUsageException("Missing timer duration.");
            if (index != args.length - 2) throw new WrongUsageException("timer <duration> must be at the end of the command.");
            return parseDuration(args[index + 1]);
        }
        if (index + 1 >= args.length) throw new WrongUsageException("Missing absolute date.");
        if (index + 2 >= args.length) throw new WrongUsageException("Missing absolute time.");
        if (index != args.length - 3) throw new WrongUsageException("date <MM-dd-yyyy> <time> must be at the end of the command.");
        return parseDate(args[index + 1], args[index + 2]);
    }

    private Schedule parseDuration(String value)
    {
        if (value.startsWith("-")) throw new WrongUsageException("Timer duration cannot be negative.");
        Matcher matcher = DURATION_PART.matcher(value);
        int end = 0;
        long[] amounts = new long[5];
        while (matcher.find())
        {
            if (matcher.start() != end) throw new WrongUsageException("Invalid timer duration: " + value);
            long amount;
            try { amount = Long.parseLong(matcher.group(1)); }
            catch (NumberFormatException e) { throw new WrongUsageException("Timer duration overflow: " + value); }
            int unit = "ydhms".indexOf(matcher.group(2).toLowerCase(Locale.ROOT));
            try { amounts[unit] = Math.addExact(amounts[unit], amount); }
            catch (ArithmeticException e) { throw new WrongUsageException("Timer duration overflow: " + value); }
            end = matcher.end();
        }
        if (end != value.length() || end == 0) throw new WrongUsageException("Invalid timer duration: " + value);
        boolean zero = true;
        for (long amount : amounts) zero &= amount == 0;
        if (zero) throw new WrongUsageException("Timer duration must be greater than zero.");
        try
        {
            ZonedDateTime expiration = ZonedDateTime.now(ZoneId.systemDefault())
                    .plusYears(amounts[0]).plusDays(amounts[1]).plusHours(amounts[2])
                    .plusMinutes(amounts[3]).plusSeconds(amounts[4]);
            return new Schedule(expiration.toInstant(), value);
        }
        catch (DateTimeException | ArithmeticException e)
        {
            throw new WrongUsageException("Timer duration overflow: " + value);
        }
    }

    private Schedule parseDate(String dateValue, String timeValue)
    {
        Matcher date = DATE.matcher(dateValue);
        if (!date.matches()) throw new WrongUsageException("Invalid date; use MM-dd-yyyy with a four-digit year.");
        Matcher time = TIME.matcher(timeValue);
        if (!time.matches()) throw new WrongUsageException("Invalid time; use a 12-hour time such as 7pm or 7:30pm.");
        try
        {
            int hour = Integer.parseInt(time.group(1));
            int minute = time.group(2) == null ? 0 : Integer.parseInt(time.group(2));
            if (hour < 1 || hour > 12 || minute > 59) throw new DateTimeException("invalid time");
            if (hour == 12) hour = 0;
            if (time.group(3).equalsIgnoreCase("pm")) hour += 12;
            LocalDate localDate = LocalDate.of(Integer.parseInt(date.group(3)), Integer.parseInt(date.group(1)), Integer.parseInt(date.group(2)));
            LocalDateTime local = LocalDateTime.of(localDate, LocalTime.of(hour, minute));
            Instant expiration = local.atZone(ZoneId.systemDefault()).toInstant();
            if (!expiration.isAfter(Instant.now())) throw new WrongUsageException("Scheduled date must be in the future.");
            return new Schedule(expiration, null);
        }
        catch (WrongUsageException e) { throw e; }
        catch (DateTimeException | NumberFormatException e)
        {
            throw new WrongUsageException("Invalid date or time; use MM-dd-yyyy and a time such as 7:30pm.");
        }
    }

    private Pair<String, BanListEntry> parseHeldMeta(ICommandSender sender, String[] args)
    {
        String dimensions = null;
        for (int i = 1; i < args.length; i++)
        {
            String candidate = args[i];
            if (!GlobalBanList.GLOBAL_NAME.equals(candidate)) Helper.parseDimIds(candidate);
            if (dimensions != null) throw new WrongUsageException("Double dimension specifiers: " + dimensions + " AND " + candidate);
            dimensions = candidate;
        }

        EntityPlayer player = getCommandSenderAsPlayer(sender);
        ItemStack stack = player.getHeldItem();
        if (stack == null) throw new WrongUsageException("The meta command requires a held item.");
        if (dimensions == null) dimensions = String.valueOf(player.dimension);
        return new Pair<>(dimensions, new BanListEntry(GameRegistry.findUniqueIdentifierFor(stack.getItem()), stack.getItemDamage()));
    }

    private Pair<String, BanListEntry> parse(ICommandSender sender, String[] args)
    {
        String dimensions = null;
        boolean wildcardOverride = false;
        int meta = OreDictionary.WILDCARD_VALUE;
        BanListEntry banListEntry = null;

        for (int i = 1; i < args.length; i++)
        {
            if (args[i].equals(GlobalBanList.GLOBAL_NAME))
            {
                dimensions = GlobalBanList.GLOBAL_NAME;
                continue;
            }
            try
            {
                Helper.parseDimIds(args[i]);
                if (dimensions != null) throw new WrongUsageException("Double dimension specifiers: " + dimensions + " AND " + args[i]);
                dimensions = args[i];
                continue;
            }
            catch (Exception ignored) {}
            try
            {
                String[] split = args[i].split(":");
                if (split.length > 3) throw new WrongUsageException("Item name not valid.");
                meta = split.length == 3 ? parseInt(sender, split[2]) : OreDictionary.WILDCARD_VALUE;
                if (banListEntry != null) throw new WrongUsageException("Double item specifiers: " + banListEntry + " AND " + args[i]);
                banListEntry = new BanListEntry(split[0] + ":" + split[1], meta);
                continue;
            }
            catch (Exception ignored) {}
            if (args[i].equals("*"))
            {
                wildcardOverride = true;
                continue;
            }
            throw new IllegalArgumentException("Not a dimension specifier or valid item: " + args[i]);
        }
        // Default to current dimension and held item
        if (dimensions == null) dimensions = String.valueOf(getCommandSenderAsPlayer(sender).dimension);
        if (banListEntry == null)
        {
            EntityPlayer player = getCommandSenderAsPlayer(sender);
            ItemStack stack = player.getHeldItem();
            if (stack == null) throw new WrongUsageException("No item specified and no item held.");
            if (wildcardOverride) meta = OreDictionary.WILDCARD_VALUE;
            banListEntry = new BanListEntry(GameRegistry.findUniqueIdentifierFor(stack.getItem()), meta);
        }
        return new Pair<>(dimensions, banListEntry);
    }

    private void list(ICommandSender sender, String[] args)
    {
        if (args.length == 1)
        {
            BanListDisplay.displayAll(sender);
        }
        else if (args[1].equalsIgnoreCase(GlobalBanList.GLOBAL_NAME)) BanListDisplay.displayGlobal(sender);
        else
        {
            BanListDisplay.displayDimension(sender, getDimension(sender, args[1]));
        }
    }

    private int getDimension(ICommandSender sender, String arg)
    {
        try
        {
            return parseInt(sender, arg);
        }
        catch (Exception e)
        {
            return getPlayer(sender, arg).dimension;
        }
    }

    public IChatComponent makeHelpText(String name, String text)
    {
        return new ChatComponentText(name).setChatStyle(new ChatStyle().setColor(AQUA)).appendSibling(new ChatComponentText(": " + text).setChatStyle(new ChatStyle().setColor(WHITE)));
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args)
    {
        if (isUsernameIndex(args, args.length)) return getListOfStringsMatchingLastWord(args, MinecraftServer.getServer().getAllUsernames());
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "reload", "pack", "unpack", "list", "publiclist", "ban", "meta", "unban", "unmeta", "allowbanneditemcraft", "disallowbanneditemcraft", "banplacementonly", "unbanplacementonly", "bancraftingonly", "unbancraftingonly");
        if (args[0].equalsIgnoreCase("publiclist") && args.length == 2)
            return getListOfStringsMatchingLastWord(args, "on", "off");
        if (args[0].equalsIgnoreCase("ban") || args[0].equalsIgnoreCase("unban"))
        {
            //noinspection unchecked
            HashSet set = new HashSet();
            //noinspection unchecked
            set.add(GlobalBanList.GLOBAL_NAME);
            //noinspection unchecked
            set.addAll(Item.itemRegistry.getKeys());
            if (args[0].equalsIgnoreCase("ban")) { set.add("timer"); set.add("date"); }
            return getListOfStringsFromIterableMatchingLastWord(args, set);
        }
        if (args[0].equalsIgnoreCase("meta") || args[0].equalsIgnoreCase("unmeta"))
        {
            if (args[0].equalsIgnoreCase("meta")) return getListOfStringsMatchingLastWord(args, GlobalBanList.GLOBAL_NAME, "timer", "date");
            return getListOfStringsMatchingLastWord(args, GlobalBanList.GLOBAL_NAME);
        }
        if (isSpecialCommand(args[0]))
        {
            HashSet set = new HashSet();
            set.add(GlobalBanList.GLOBAL_NAME);
            set.addAll(Item.itemRegistry.getKeys());
            return getListOfStringsFromIterableMatchingLastWord(args, set);
        }
        return null;
    }

    private boolean isSpecialCommand(String command)
    {
        return command.equalsIgnoreCase("allowbanneditemcraft")
                || command.equalsIgnoreCase("disallowbanneditemcraft")
                || command.equalsIgnoreCase("banplacementonly")
                || command.equalsIgnoreCase("unbanplacementonly")
                || command.equalsIgnoreCase("bancraftingonly")
                || command.equalsIgnoreCase("unbancraftingonly");
    }

    @Override
    public boolean isUsernameIndex(String[] args, int arg)
    {
        if (args.length == 0) return false;
        switch (args[0].toLowerCase())
        {
            case "unpack":
            case "pack":
            case "list":
                return arg == 2;
        }
        return false;
    }
}
