package de.joker.randomizer.utils;

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;
import java.util.WeakHashMap;

public class MessageUtils {
    private static final MiniMessage mm = MiniMessage.miniMessage();
    @Getter
    private final static String name = "<gradient:#ff0000:#ff9900>Randomizer</gradient>";
    private final static String prefix = name + "<color:#30303d> • <color:#b2c2d4>";
    
    private static final Map<Audience, Long> cooldowns = new WeakHashMap<>();
    private static final long COOLDOWN_MS = 1000;

    public static Component parseWithPrefix(String message) {
        return parse(prefix + message);
    }

    public static Component parse(String message) {
        return mm.deserialize(message).decoration(TextDecoration.ITALIC, false);
    }

    public static void send(Audience audience, String message) {
        if (canSend(audience)) {
            audience.sendMessage(parseWithPrefix(message));
            cooldowns.put(audience, System.currentTimeMillis());
        }
    }

    public static void sendRaw(Audience audience, String message) {
        if (canSend(audience)) {
            audience.sendMessage(parse(message));
            cooldowns.put(audience, System.currentTimeMillis());
        }
    }

    private static boolean canSend(Audience audience) {
        long now = System.currentTimeMillis();
        return cooldowns.getOrDefault(audience, 0L) + COOLDOWN_MS <= now;
    }
}