package com.totemskins;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class TotemSkins {
    private TotemSkins() {}

    private static final Map<String, Identifier> CACHE = new HashMap<>();   // успех
    private static final Set<String> FAILED = new HashSet<>();              // нет такого ника (не запрашивать снова)
    private static final Set<String> PENDING = Collections.synchronizedSet(new HashSet<>());

    /** Тотем с именем игрока -> Identifier «тотема-из-скина». Онлайн — сразу; иначе грузим с Mojang (с дебаунсом). */
    public static Identifier resolveTotemTexture(ItemStack stack) {
        if (stack == null || !stack.isOf(Items.TOTEM_OF_UNDYING)) return null;
        Text name = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (name == null) return null;
        String nick = name.getString().trim();
        if (nick.isEmpty() || nick.length() > 16) return null;  // ники Minecraft <= 16 символов

        Identifier cached = CACHE.get(nick);
        if (cached != null) return cached;

        MinecraftClient mc = MinecraftClient.getInstance();
        // быстрый путь: игрок онлайн на сервере — скин уже есть, без запросов к Mojang
        if (mc.getNetworkHandler() != null) {
            PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(nick);
            if (e != null) {
                Identifier id = buildFromSkin(nick, e.getSkinTextures());
                if (id != null) { CACHE.put(nick, id); return id; }
            }
        }

        if (FAILED.contains(nick)) return null;                 // уже знаем, что ника нет — не дёргаем повторно

        requestAsync(nick);                                     // живой запрос (каждый ник — один раз, потом из кэша)
        return null;
    }

    private static void requestAsync(String nick) {
        if (!PENDING.add(nick)) return; // уже грузится
        MinecraftClient mc = MinecraftClient.getInstance();
        CompletableFuture
                .supplyAsync(() -> {
                    try { return mc.getApiServices().profileResolver().getProfileByName(nick); }
                    catch (Exception ex) { return Optional.<GameProfile>empty(); }
                }, Util.getIoWorkerExecutor())
                .thenComposeAsync(opt -> opt
                        .map(p -> mc.getSkinProvider().fetchSkinTextures(p))
                        .orElseGet(() -> CompletableFuture.completedFuture(Optional.empty())),
                        Util.getIoWorkerExecutor())
                .thenAcceptAsync(skinOpt -> {
                    Identifier id = skinOpt.map(st -> buildFromSkin(nick, st)).orElse(null);
                    if (id != null) CACHE.put(nick, id);
                    else FAILED.add(nick);          // ника нет / скин недоступен — больше не дёргаем
                    PENDING.remove(nick);
                }, mc)
                .exceptionally(ex -> { FAILED.add(nick); PENDING.remove(nick); return null; });
    }

    /** Достаёт NativeImage скина, генерит текстуру тотема, регистрирует. ТОЛЬКО на рендер-потоке. */
    private static Identifier buildFromSkin(String nick, SkinTextures st) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Identifier skinId = st.body().texturePath();
        AbstractTexture tex = mc.getTextureManager().getTexture(skinId);
        if (!(tex instanceof NativeImageBackedTexture nibt)) return null;
        NativeImage skin = nibt.getImage();
        if (skin == null || skin.getWidth() < 64 || skin.getHeight() < 64) return null;

        NativeImage totem = SkinToTotem.generate(skin);
        String safe = nick.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        Identifier id = Identifier.of("totem-skins", "generated/" + safe);
        mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "totem-skin/" + nick, totem));
        return id;
    }
}
