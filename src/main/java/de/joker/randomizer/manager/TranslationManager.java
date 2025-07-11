package de.joker.randomizer.manager;

import de.joker.randomizer.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;

@Slf4j
public class TranslationManager {
    private final Map<String, List<Translation>> translations = new HashMap<>();

    public TranslationManager() {
        init();
    }

    public Optional<Translation> getTranslation(String language, String key) {
        Optional<Translation> translation = translations
                .getOrDefault(language, Collections.emptyList())
                .stream()
                .filter(t -> t.key().equals(key))
                .findFirst();

        if (translation.isPresent()) {
            return translation;
        }

        log.warn("Translation for key '{}' in language '{}' not found, falling back to English.", key, language);

        return translations
                .getOrDefault(Locale.ENGLISH.toLanguageTag(), Collections.emptyList())
                .stream()
                .filter(t -> t.key().equals(key))
                .findFirst();
    }

    public Component getTranslationComponent(Locale language, String key, TagResolver... tagResolvers) {
        Optional<Translation> translation = getTranslation(language.toLanguageTag(), key);

        if (translation.isPresent()) {
            return MessageUtils.parse(translation.get().value(), tagResolvers);
        } else {
            log.warn("Translation for key '{}' in language '{}' not found.", key, language.toLanguageTag());
            return MessageUtils.parse(
                    String.format("<red>Translation missing for key <gray>'%s' <red>in language <gray>'%s'<red>.", key, language)
            );
        }
    }

    public Component translate(Audience player, String key, TagResolver... tagResolvers) {
        Locale language = player instanceof Player ? ((Player) player).locale() : Locale.ENGLISH;

        return getTranslationComponent(language, key, tagResolvers);
    }

    public void send(Audience player, String key, TagResolver... tagResolvers) {
        Component translation = translate(player, key, tagResolvers);

        player.sendMessage(translation);
    }

    public void sendPrefixed(Audience player, String key, TagResolver... tagResolvers) {
        Component translation = translate(player, key, tagResolvers);

        player.sendMessage(MessageUtils.addPrefix(translation));
    }

    public void reloadTranslations() {
        translations.clear();
        File translationsFile = new File(getClass().getClassLoader().getResource("lang").getFile());
        if (!translationsFile.exists()) {
            log.error("Translations directory does not exist: {}", translationsFile.getAbsolutePath());
            return;
        }

        File[] files = translationsFile.listFiles((dir, name) -> name.endsWith(".yaml"));
        if (files == null || files.length == 0) {
            log.error("No translation files found in {}", translationsFile.getAbsolutePath());
            return;
        }

        for (File file : files) {
            log.info("Loading translations from file: {}", file.getName());
            String languageCode = file.getName().replace(".yaml", "");

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            for (String key : config.getKeys(true)) {
                String message = config.getString(key);
                if (message != null) {
                    Translation translation = new Translation(key, message);
                    translations.computeIfAbsent(languageCode, k -> new ArrayList<>()).add(translation);
                } else {
                    log.error("Translation key '{}' in file '{}' is null.", key, file.getName());
                }
            }

            log.info("Loaded {} translations for language '{}'.", translations.get(languageCode).size(), languageCode);
        }

        log.info("Loaded {} languages with translations.", translations.size());
    }

    private void init() {
        reloadTranslations();
    }

    public record Translation(String key, String value) {}
}