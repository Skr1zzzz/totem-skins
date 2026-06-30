package com.totemskins;

import net.minecraft.util.Identifier;

/** Навешивается на ItemRenderState — перетекстуривает уже собранные ванильные квады тотема. */
public interface SkinHolder {
    void totemskins$retexture(Identifier tex);
}
