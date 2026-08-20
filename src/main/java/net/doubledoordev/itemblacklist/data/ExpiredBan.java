package net.doubledoordev.itemblacklist.data;

/** Describes a timed world ban removed by expiration cleanup. */
public final class ExpiredBan
{
    private final BanListEntry entry;
    private final String dimensions;

    public ExpiredBan(BanListEntry entry, String dimensions)
    {
        this.entry = entry;
        this.dimensions = dimensions;
    }

    public BanListEntry getEntry()
    {
        return entry;
    }

    public String getDimensions()
    {
        return dimensions;
    }
}
