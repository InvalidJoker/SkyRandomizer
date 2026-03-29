package de.joker.randomizer.utils;

import de.joker.randomizer.manager.LocalizationManager;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Map;
import java.util.WeakHashMap;

public class MessageUtils {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static LocalizationManager localizationManager;

    @Getter
    private static final String name = "<gradient:#ff0000:#ff9900>Randomizer</gradient>";
    private static final String prefix = name + "<color:#30303d> | <color:#b2c2d4>";

    private static final Map<Audience, Long> cooldowns = new WeakHashMap<>();
    private static final long COOLDOWN_MS = 1000;

    public static void init(LocalizationManager localizationManager) {
        MessageUtils.localizationManager = localizationManager;
    }

    public static Component parseWithPrefix(String message) {
        return parse(prefix + message);
    }

    public static Component parse(String message) {
        return MM.deserialize(message).decoration(TextDecoration.ITALIC, false);
    }

    public static Component component(Audience audience, String key, MessagePlaceholder... placeholders) {
        return parse(localized(audience, key, placeholders));
    }

    public static void send(Audience audience, String key, MessagePlaceholder... placeholders) {
        if (canSend(audience)) {
            audience.sendMessage(parseWithPrefix(localized(audience, key, placeholders)));
            cooldowns.put(audience, System.currentTimeMillis());
        }
    }

    public static void sendRaw(Audience audience, String key, MessagePlaceholder... placeholders) {
        if (canSend(audience)) {
            audience.sendMessage(component(audience, key, placeholders));
            cooldowns.put(audience, System.currentTimeMillis());
        }
    }

    public static String localized(Audience audience, String key, MessagePlaceholder... placeholders) {
        if (localizationManager == null) {
            return key;
        }
        return localizationManager.getMessage(audience, key, placeholders);
    }

    public static MessagePlaceholder placeholder(String key, Object value) {
        return MessagePlaceholder.of(key, value);
    }

    private static boolean canSend(Audience audience) {
        long now = System.currentTimeMillis();
        return cooldowns.getOrDefault(audience, 0L) + COOLDOWN_MS <= now;
    }
}
