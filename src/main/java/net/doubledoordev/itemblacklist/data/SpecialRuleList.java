package net.doubledoordev.itemblacklist.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.doubledoordev.itemblacklist.Helper;
import net.doubledoordev.itemblacklist.ItemBlacklist;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** A permanent, world-local item rule list with the normal dimension syntax. */
public final class SpecialRuleList
{
    public static final String CRAFT_ALLOW_FILE = "ItemBlacklistCraftAllow.json";
    public static final String PLACEMENT_ONLY_FILE = "ItemBlacklistPlacementOnly.json";
    public static final String CRAFTING_ONLY_FILE = "ItemBlacklistCraftingOnly.json";

    public static SpecialRuleList craftAllow;
    public static SpecialRuleList placementOnly;
    public static SpecialRuleList craftingOnly;

    private final Multimap<Integer, BanList> dimensionMap = HashMultimap.create();
    private BanList global = new BanList(GlobalBanList.GLOBAL_NAME);
    private final File file;

    private SpecialRuleList(File file)
    {
        this.file = file;
    }

    public static void init()
    {
        File world = MinecraftServer.getServer().worldServers[0].getSaveHandler().getWorldDirectory();
        craftAllow = load(new File(world, CRAFT_ALLOW_FILE));
        placementOnly = load(new File(world, PLACEMENT_ONLY_FILE));
        craftingOnly = load(new File(world, CRAFTING_ONLY_FILE));
    }

    private static SpecialRuleList load(File file)
    {
        SpecialRuleList result = new SpecialRuleList(file);
        if (!file.exists()) return result;
        try
        {
            JsonElement root = Helper.GSON_PARSER.parse(FileUtils.readFileToString(file, "UTF-8"));
            if (root == null || !root.isJsonObject()) throw new JsonParseException("Root must be a JSON object.");
            for (Map.Entry<String, JsonElement> property : root.getAsJsonObject().entrySet())
            {
                BanList list = Helper.GSON.fromJson(property.getValue(), BanList.class);
                list.setDimension(property.getKey());
                for (BanListEntry entry : list.banListEntryMap.values())
                    if (!entry.isPermanent()) throw new JsonParseException("Special rules cannot have expiresAt.");
                if (GlobalBanList.GLOBAL_NAME.equals(property.getKey())) result.global = list;
                else for (int dimension : list.getDimIds()) result.dimensionMap.put(dimension, list);
            }
            return result;
        }
        catch (Exception e)
        {
            String message = "Invalid special item rule file " + file.getAbsolutePath() + ".";
            ItemBlacklist.logger.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public boolean matches(int dimension, ItemStack stack)
    {
        if (stack == null || stack.getItem() == null) return false;
        if (global.isBanned(stack)) return true;
        for (BanList list : dimensionMap.get(dimension)) if (list.isBanned(stack)) return true;
        return false;
    }

    public void add(String dimensions, BanListEntry entry)
    {
        BanList list = find(dimensions);
        if (list == null)
        {
            list = new BanList(dimensions);
            for (int dimension : list.getDimIds()) dimensionMap.put(dimension, list);
        }
        list.banListEntryMap.put(entry.getItem(), entry);
        save();
    }

    public boolean remove(String dimensions, BanListEntry entry)
    {
        BanList list = find(dimensions);
        if (list == null || !list.banListEntryMap.remove(entry.getItem(), entry)) return false;
        save();
        return true;
    }

    private BanList find(String dimensions)
    {
        if (GlobalBanList.GLOBAL_NAME.equals(dimensions)) return global;
        BanList match = null;
        for (BanList list : new HashSet<BanList>(dimensionMap.values()))
        {
            if (!list.getDimension().equals(dimensions)) continue;
            if (match != null) throw new IllegalStateException("Duplicate special-rule dimension key: " + dimensions);
            match = list;
        }
        return match;
    }

    private void save()
    {
        JsonObject root = new JsonObject();
        root.add(GlobalBanList.GLOBAL_NAME, Helper.GSON.toJsonTree(global, BanList.class));
        for (BanList list : new HashSet<BanList>(dimensionMap.values()))
            root.add(list.getDimension(), Helper.GSON.toJsonTree(list, BanList.class));
        try
        {
            FileUtils.writeStringToFile(file, Helper.GSON.toJson(root), "UTF-8");
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not save " + file.getAbsolutePath(), e);
        }
    }

    public Set<BanList> allLists()
    {
        Set<BanList> lists = new HashSet<BanList>(dimensionMap.values());
        lists.add(global);
        return lists;
    }

    public Set<BanList> listsForDimension(int dimension)
    {
        return new HashSet<BanList>(dimensionMap.get(dimension));
    }

    public BanList getGlobal()
    {
        return global;
    }

    public Set<String> getDimensionNames()
    {
        Set<String> names = new HashSet<String>();
        names.add(GlobalBanList.GLOBAL_NAME);
        for (BanList list : dimensionMap.values()) names.add(list.getDimension());
        return names;
    }
}
