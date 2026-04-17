package com.sunwayMinecraft.residency.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Message {
    private Message() {}

    public static Component info(String text) {
        return Component.text(text, NamedTextColor.AQUA);
    }

    public static Component ok(String text) {
        return Component.text(text, NamedTextColor.GREEN);
    }

    public static Component warn(String text) {
        return Component.text(text, NamedTextColor.YELLOW);
    }

    public static Component error(String text) {
        return Component.text(text, NamedTextColor.RED);
    }
}
