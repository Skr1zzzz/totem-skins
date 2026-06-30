package com.totemskins;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotemSkinsClient implements ClientModInitializer {
    public static final String MOD_ID = "totem-skins";
    public static final Logger LOG = LoggerFactory.getLogger("Totem Skins");

    @Override
    public void onInitializeClient() {
        LOG.info("[Totem Skins] client initialized");
    }
}
