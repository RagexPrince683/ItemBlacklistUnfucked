package net.doubledoordev.itemblacklist.mixin;

import net.doubledoordev.itemblacklist.util.CraftingResultHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stops every click mode before vanilla can extract a banned craft result. */
@Mixin(Container.class)
public abstract class MixinContainer
{
    @Inject(method = "slotClick", at = @At("HEAD"), cancellable = true)
    private void itemBlacklist$blockBannedCraftingResult(int slotId, int clickedButton, int clickMode,
            EntityPlayer player, CallbackInfoReturnable<ItemStack> callback)
    {
        if (CraftingResultHandler.isBlockedCraftingResult(player, (Container) (Object) this, slotId))
        {
            callback.setReturnValue(null);
        }
    }
}
