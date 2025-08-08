package net.doubledoordev.itemblacklist.client;

import net.doubledoordev.itemblacklist.util.ItemBlacklisted;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.GL11;

import static net.minecraftforge.client.IItemRenderer.ItemRenderType.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author Dries007
 */
public class Renderer implements IItemRenderer
{
    private EntityItem entityItem = new EntityItem(Minecraft.getMinecraft().theWorld);
    private RenderItem rendererItem = new RenderItem()
    {
        @Override
        public boolean shouldSpreadItems()
        {
            return false;
        }

        @Override
        public boolean shouldBob()
        {
            return false;
        }
    };

    {
        rendererItem.setRenderManager(RenderManager.instance);
    }

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type)
    {
        if (!ItemBlacklisted.canUnpack(item)) return false;
        ItemStack unpacked = ItemBlacklisted.unpack(item);
        if (unpacked == item) return false;
        IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(unpacked, type);
        if (renderer != null) return renderer.handleRenderType(unpacked, type);
        return unpacked.getItem().getSpriteNumber() != ItemBlacklisted.I.getSpriteNumber();
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper)
    {
        ItemStack unpacked = ItemBlacklisted.unpack(item);
        if (unpacked == item) return helper != ItemRendererHelper.INVENTORY_BLOCK;
        IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(unpacked, type);
        if (renderer != null) return renderer.shouldUseRenderHelper(type, unpacked, helper);
        return helper != ItemRendererHelper.INVENTORY_BLOCK;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data)
    {
        ItemStack unpacked = ItemBlacklisted.unpack(item);
        if (unpacked == item) return;
        if (type == EQUIPPED || type == EQUIPPED_FIRST_PERSON || type == INVENTORY) unpacked.stackSize = 1;
        IItemRenderer renderer = MinecraftForgeClient.getItemRenderer(unpacked, type);
        if (renderer != null)
        {
            renderer.renderItem(type, unpacked, data);
            return;
        }

        try
        {
            glPushMatrix();

            float scale = 4f;

            if (type == INVENTORY)
            {
                scale = 4f;
                glPushMatrix();
                GL11.glTranslatef(-2, +3, -3.0F);
                GL11.glScalef(10F, 10F, 10F);
                GL11.glTranslatef(1.0F, 0.5F, 1.0F);
                GL11.glScalef(1.0F, 1.0F, -1F);
                GL11.glRotatef(210F, 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(45F, 0.0F, 1.0F, 0.0F);
            }
            else if (type == ENTITY)
            {
                scale = 2f;
            }
            else if (type == EQUIPPED_FIRST_PERSON)
            {
                GL11.glTranslatef(0.6f, 0.65f, 0.5f);
            }
            else if (type == EQUIPPED)
            {
                GL11.glTranslatef(0.6f, 0.3f, 0.6f);
            }

            glScalef(scale, scale, scale);

            entityItem.setEntityItemStack(unpacked);

            RenderHelper.enableStandardItemLighting();
            rendererItem.doRender(entityItem, 0, 0, 0, 0, 0);
            RenderHelper.disableStandardItemLighting();

            if (type == INVENTORY)
            {
                glPopMatrix();

                glPushMatrix();
                glDisable(GL_DEPTH_TEST);
                Minecraft.getMinecraft().getTextureManager().bindTexture(Minecraft.getMinecraft().getTextureManager().getResourceLocation(ItemBlacklisted.I.getSpriteNumber()));
                rendererItem.renderIcon(0, 0, item.getIconIndex(), 16, 16);
                glEnable(GL_DEPTH_TEST);
                glPopMatrix();
            }

            glPopMatrix();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
