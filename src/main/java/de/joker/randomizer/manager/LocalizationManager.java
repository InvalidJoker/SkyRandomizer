package de.joker.randomizer.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.cytooxien.realms.api.PlayerInformationProvider;
import de.cytooxien.realms.api.enums.Language;
import de.joker.randomizer.SkyRandomizer;
import de.joker.randomizer.utils.MessagePlaceholder;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Slf4j
public class LocalizationManager {

    public static final String FALLBACK_LOCALE = "en";

    private static final Gson GSON = new Gson();
    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();

    private final SkyRandomizer plugin;
    private final Supplier<PlayerInformationProvider> playerInformationProviderSupplier;
    private final Map<String, Map<String, String>> translations = new ConcurrentHashMap<>();

    public LocalizationManager(SkyRandomizer plugin, Supplier<PlayerInformationProvider> playerInformationProviderSupplier) {
        this.plugin = plugin;
        this.playerInformationProviderSupplier = playerInformationProviderSupplier;
        loadTranslations();
    }

    public String getMessage(Audience audience, String key, MessagePlaceholder... placeholders) {
        return getMessage(resolveLocale(audience), key, placeholders);
    }

    public String getMessage(String locale, String key, MessagePlaceholder... placeholders) {
        Map<String, String> fallback = translations.getOrDefault(FALLBACK_LOCALE, Map.of());
        Map<String, String> localized = translations.getOrDefault(locale, fallback);
        String template = localized.getOrDefault(key, fallback.getOrDefault(key, key));

        for (MessagePlaceholder placeholder : placeholders) {
            template = template.replace("{" + placeholder.key() + "}", placeholder.value());
        }

        return template;
    }

    public String resolveLocale(Audience audience) {
        if (audience instanceof Player player) {
            return resolveLocale(player.getUniqueId());
        }
        return FALLBACK_LOCALE;
    }

    public String resolveLocale(UUID uniqueId) {
        PlayerInformationProvider provider = playerInformationProviderSupplier.get();
        if (provider == null) {
            return FALLBACK_LOCALE;
        }

        try {
            return mapLanguage(provider.language(uniqueId));
        } catch (Exception e) {
            log.debug("Could not resolve player language for {}", uniqueId, e);
            return FALLBACK_LOCALE;
        }
    }

    private String mapLanguage(Language language) {
        if (language == null) {
            return FALLBACK_LOCALE;
        }

        return switch (language) {
            case DE, DE_AT, DE_CH, MXN -> "de";
            default -> FALLBACK_LOCALE;
        };
    }

    private void loadTranslations() {
        translations.clear();

        Path codeSourcePath = getCodeSourcePath();
        if (codeSourcePath != null) {
            if (Files.isDirectory(codeSourcePath)) {
                loadFromRoot(codeSourcePath.resolve("lang"));
            } else {
                try (FileSystem fileSystem = FileSystems.newFileSystem(codeSourcePath, (ClassLoader) null)) {
                    loadFromRoot(fileSystem.getPath("/lang"));
                } catch (IOException e) {
                    log.warn("Could not load localization files from {}", codeSourcePath, e);
                }
            }
        }

        if (translations.isEmpty()) {
            loadKnownFile("en");
            loadKnownFile("de");
        }

        translations.putIfAbsent(FALLBACK_LOCALE, new ConcurrentHashMap<>());
    }

    private Path getCodeSourcePath() {
        try {
            CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }

            return Paths.get(codeSource.getLocation().toURI());
        } catch (Exception e) {
            log.warn("Could not determine plugin code source for localization", e);
            return null;
        }
    }

    private void loadFromRoot(Path langRoot) {
        if (langRoot == null || !Files.exists(langRoot)) {
            return;
        }

        try (Stream<Path> pathStream = Files.walk(langRoot)) {
            pathStream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> loadTranslationFile(langRoot, path));
        } catch (IOException e) {
            log.warn("Could not walk localization root {}", langRoot, e);
        }
    }

    private void loadTranslationFile(Path langRoot, Path path) {
        try {
            Path relative = langRoot.relativize(path);
            if (relative.getNameCount() < 2) {
                return;
            }

            String locale = relative.getName(0).toString().toLowerCase(Locale.ROOT);
            try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
                Map<String, String> values = GSON.fromJson(reader, STRING_MAP_TYPE);
                if (values != null) {
                    translations.computeIfAbsent(locale, ignored -> new ConcurrentHashMap<>()).putAll(values);
                }
            }
        } catch (Exception e) {
            log.warn("Could not load translation file {}", path, e);
        }
    }

    private void loadKnownFile(String locale) {
        String resourcePath = "lang/" + locale + "/messages.json";
        try (var inputStream = plugin.getResource(resourcePath)) {
            if (inputStream == null) {
                return;
            }

            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                Map<String, String> values = GSON.fromJson(reader, STRING_MAP_TYPE);
                if (values != null) {
                    translations.computeIfAbsent(locale, ignored -> new ConcurrentHashMap<>()).putAll(values);
                }
            }
        } catch (IOException e) {
            log.warn("Could not load fallback localization file {}", resourcePath, e);
        }
    }
}
