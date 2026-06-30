package com.totemskins.mixin;

import com.totemskins.SkinHolder;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.SkullBlockEntityRenderer;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(ItemRenderState.class)
public abstract class ItemRenderStateMixin implements SkinHolder {

    @Shadow private ItemRenderState.LayerRenderState[] layers;
    @Shadow private int layerCount;
    @Shadow public abstract void addModelKey(Object key);

    @Override
    public void totemskins$retexture(Identifier tex) {
        // различаем кэш: обычный тотем и кастомный (и разные ники) — отдельные состояния
        addModelKey(tex);
        RenderLayer layer = SkullBlockEntityRenderer.getCutoutRenderLayer(SkullBlock.Type.PLAYER, tex);
        for (int i = 0; i < layerCount; i++) {
            ItemRenderState.LayerRenderState lrs = layers[i];
            List<BakedQuad> quads = lrs.getQuads();
            for (int q = 0; q < quads.size(); q++) {
                quads.set(q, totemskins$remap(quads.get(q)));
            }
            lrs.setRenderLayer(layer);
        }
    }

    /** Пересчитывает UV квада из атласных координат спрайта в 0..1 (под мою отдельную текстуру). Позиции не трогает. */
    @Unique
    private static BakedQuad totemskins$remap(BakedQuad src) {
        Sprite sp = src.sprite();
        float uMin = sp.getMinU(), uMax = sp.getMaxU(), vMin = sp.getMinV(), vMax = sp.getMaxV();
        float du = uMax - uMin, dv = vMax - vMin;
        long[] uv = new long[4];
        for (int i = 0; i < 4; i++) {
            long packed = src.getTexcoords(i);
            float u = Float.intBitsToFloat((int) (packed >>> 32));
            float v = Float.intBitsToFloat((int) (packed & 0xFFFFFFFFL));
            float nu = du != 0 ? (u - uMin) / du : u;
            float nv = dv != 0 ? (v - vMin) / dv : v;
            uv[i] = ((long) Float.floatToRawIntBits(nu) << 32) | (Float.floatToRawIntBits(nv) & 0xFFFFFFFFL);
        }
        return new BakedQuad(src.position0(), src.position1(), src.position2(), src.position3(),
                uv[0], uv[1], uv[2], uv[3], src.tintIndex(), src.face(), src.sprite(), src.shade(), src.lightEmission());
    }
}
