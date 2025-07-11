package de.joker.randomizer.utils;

import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MessageUtils {
    private static final MiniMessage mm = MiniMessage.miniMessage();
    @Getter
    private final static String name = "<gradient:#ff0000:#ff9900>Randomizer</gradient>";
    private final static String prefix = name + "<color:#30303d> • <color:#b2c2d4>";


    public static Component parseWithPrefix(String message) {
        return parse(prefix + " " + message);
    }

    public static Component parse(String message, TagResolver... tagResolvers) {
        return mm.deserialize(message, tagResolvers).decoration(TextDecoration.ITALIC, false);
    }

    public static Component addPrefix(Component message) {
        return mm.deserialize(prefix + " ").append(message).decoration(TextDecoration.ITALIC, false);
    }

    public static void send(Audience audience, String message) {
        audience.sendMessage(parseWithPrefix(message));
    }

    public static void sendRaw(Audience audience, String message) {
        audience.sendMessage(parse(message));
    }
}