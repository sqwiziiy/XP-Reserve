package com.mentality.xpreserve;

import com.mentality.xpreserve.component.ModDataComponents;
import com.mentality.xpreserve.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common bootstrap for XP Reserve. */
public final class XpReserveMod implements ModInitializer {
    public static final String MOD_ID = "xp-reserve";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModDataComponents.initialize();
        ModItems.initialize();
        LOGGER.info("Mentalitys | XP Reserve initialized!");
    }
}
