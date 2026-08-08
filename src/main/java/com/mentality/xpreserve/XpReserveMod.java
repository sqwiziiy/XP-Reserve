package com.mentality.xpreserve;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common bootstrap for XP Reserve. */
public final class XpReserveMod implements ModInitializer {
    public static final String MOD_ID = "xp-reserve";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Mentalitys | XP Reserve initialized!");
    }
}
