package net.doubledoordev.itemblacklist.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

/** Registers the sole transformer used by ItemBlacklist. */
@IFMLLoadingPlugin.Name("ItemBlacklistCore")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("net.doubledoordev.itemblacklist.core")
public final class ItemBlacklistLoadingPlugin implements IFMLLoadingPlugin
{
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[] { ContainerSlotClickTransformer.class.getName() };
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
