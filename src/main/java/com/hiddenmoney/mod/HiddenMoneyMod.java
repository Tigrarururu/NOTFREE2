package com.hiddenmoney.mod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HiddenMoneyMod implements ModInitializer {
    public static final String MOD_ID = "hidden-money-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Мод инициализируется, но не показывает никаких сообщений в чате
        // Все механики скрыты от пользователя
    }
}
