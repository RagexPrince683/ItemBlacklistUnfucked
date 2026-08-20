package net.doubledoordev.itemblacklist.data;

import com.google.gson.*;
import cpw.mods.fml.common.registry.GameRegistry;
import net.doubledoordev.itemblacklist.util.ItemBlacklisted;
import net.minecraft.item.Item;

import java.lang.reflect.Type;

import static net.minecraftforge.oredict.OreDictionary.WILDCARD_VALUE;

/**
 * @author Dries007
 */
public class BanListEntry
{
    private Item item;
    private int meta = 0;

    public BanListEntry(GameRegistry.UniqueIdentifier uid, int meta)
    {
        this.item = GameRegistry.findItem(uid.modId, uid.name);
        if (this.item == ItemBlacklisted.I) throw new IllegalArgumentException("You can't ban the banning item.");
        this.meta = meta;
        if (item == null) throw new IllegalArgumentException(uid.toString() + " isn't a valid item.");
    }

    public BanListEntry(String name, int meta)
    {
        this(new GameRegistry.UniqueIdentifier(name), meta);
    }

    public boolean isBanned(int meta)
    {
        return WILDCARD_VALUE == this.meta || meta == this.meta;
    }

    public Item getItem()
    {
        return item;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BanListEntry that = (BanListEntry) o;

        return meta == that.meta && item.equals(that.item);
    }

    @Override
    public int hashCode()
    {
        int result = item.hashCode();
        result = 31 * result + meta;
        return result;
    }

    @Override
    public String toString()
    {
        return GameRegistry.findUniqueIdentifierFor(item).toString() + ':' + (meta == WILDCARD_VALUE ? "*" : String.valueOf(meta));
    }

    public static class Json implements JsonSerializer<BanListEntry>, JsonDeserializer<BanListEntry>
    {
        @Override
        public BanListEntry deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject object = json.getAsJsonObject();
            int meta = WILDCARD_VALUE;
            boolean hasMeta = object.has("meta");
            boolean hasLegacyDamage = object.has("damage");
            if (hasMeta)
            {
                meta = parseMeta(object.get("meta"), "meta");
            }
            if (hasLegacyDamage)
            {
                int legacyMeta = parseMeta(object.get("damage"), "damage");
                if (hasMeta && meta != legacyMeta)
                {
                    throw new JsonParseException("Conflicting 'meta' and legacy 'damage' values for item "
                            + object.get("item").getAsString() + ".");
                }
                meta = legacyMeta;
            }
            return new BanListEntry(object.get("item").getAsString(), meta);
        }

        private int parseMeta(JsonElement element, String property) throws JsonParseException
        {
            String value = element.getAsString();
            if ("*".equals(value)) return WILDCARD_VALUE;
            try
            {
                return Integer.parseInt(value);
            }
            catch (NumberFormatException e)
            {
                throw new JsonParseException("Invalid '" + property + "' metadata value: " + value, e);
            }
        }

        @Override
        public JsonElement serialize(BanListEntry src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("item", GameRegistry.findUniqueIdentifierFor(src.item).toString());
            if (src.meta == WILDCARD_VALUE) jsonObject.addProperty("meta", "*");
            else jsonObject.addProperty("meta", src.meta);
            return jsonObject;
        }
    }
}
