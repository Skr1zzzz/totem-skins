package com.totemskins.mixin;

import com.totemskins.SkinHolder;
import com.totemskins.TotemSkins;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {

    // после сборки состояния перетекстуриваем ванильные квады тотема (геометрия 1-в-1, только текстура)
    @Inject(method = "clearAndUpdate", at = @At("TAIL"))
    private void totemskins$tag(ItemRenderState state, ItemStack stack, ItemDisplayContext ctx,
                               World world, HeldItemContext held, int seed, CallbackInfo ci) {
        Identifier tex = TotemSkins.resolveTotemTexture(stack);
        if (tex != null) {
            ((SkinHolder) state).totemskins$retexture(tex);
        }
    }
}
