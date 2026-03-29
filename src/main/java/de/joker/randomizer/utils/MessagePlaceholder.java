package de.joker.randomizer.utils;

public record MessagePlaceholder(String key, String value) {

    public static MessagePlaceholder of(String key, Object value) {
        return new MessagePlaceholder(key, String.valueOf(value));
    }
}
