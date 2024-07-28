package org.sRandomRTP.Files;

import com.google.common.base.Preconditions;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.bukkit.plugin.Plugin;
import org.sRandomRTP.DifferentMethods.Variables;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ConfigUpdater {
    private static final char SEPARATOR = '.';
    public static boolean update(String resourceName, File toUpdate, String... ignoredSections) throws IOException {
        return update(Variables.getInstance(), resourceName, toUpdate, Arrays.asList(ignoredSections));
    }
    public static boolean update(Plugin plugin, String resourceName, File toUpdate, List<String> ignoredSections) throws IOException {
        Preconditions.checkArgument(toUpdate.exists(), "The toUpdate file doesn't exist!");
        InputStream resourceStream = plugin.getResource(resourceName);
        if (resourceStream == null) {
            System.err.println("Resource not found: " + resourceName);
            return false;
        }
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader((plugin.getResource(resourceName)), StandardCharsets.UTF_8));
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(new FileInputStream(toUpdate), StandardCharsets.UTF_8));
        Map<String, String> comments = parseComments(plugin, resourceName, defaultConfig);
        Map<String, String> ignoredSectionsValues = parseIgnoredSections(toUpdate, comments, ignoredSections == null ? Collections.emptyList() : ignoredSections);
        StringWriter writer = new StringWriter();
        write(defaultConfig, currentConfig, new BufferedWriter(writer), comments, ignoredSectionsValues);
        String value = writer.toString();
        Path toUpdatePath = toUpdate.toPath();
        String existingContent = new String(Files.readAllBytes(toUpdatePath), StandardCharsets.UTF_8);
        String updatedContent = value;
        if (!existingContent.equals(updatedContent)) {
            Files.write(toUpdatePath, updatedContent.getBytes(StandardCharsets.UTF_8));
        }
        if (!existingContent.equals(updatedContent)) {
            Files.write(toUpdatePath, updatedContent.getBytes(StandardCharsets.UTF_8));
            return true;
        }
        return false;
    }
    private static void write(FileConfiguration defaultConfig, FileConfiguration currentConfig, BufferedWriter writer, Map<String, String> comments, Map<String, String> ignoredSectionsValues) throws IOException {
        FileConfiguration parserConfig = new YamlConfiguration();
        keyLoop: for (String fullKey : defaultConfig.getKeys(true)) {
            String indents = KeyBuilder.getIndents(fullKey, SEPARATOR);
            if (ignoredSectionsValues.isEmpty()) {
                writeCommentIfExists(comments, writer, fullKey, indents);
            } else {
                for (Map.Entry<String, String> entry : ignoredSectionsValues.entrySet()) {
                    if (entry.getKey().equals(fullKey)) {
                        writer.write(ignoredSectionsValues.get(fullKey));
                        continue keyLoop;
                    } else if (KeyBuilder.isSubKeyOf(entry.getKey(), fullKey, SEPARATOR)) {
                        continue keyLoop;
                    }
                }
                writeCommentIfExists(comments, writer, fullKey, indents);
            }
            Object currentValue = currentConfig.get(fullKey);
            if (currentValue == null)
                currentValue = defaultConfig.get(fullKey);
            String[] splitFullKey = fullKey.split("[" + SEPARATOR + "]");
            String trailingKey = splitFullKey[splitFullKey.length - 1];
            if (currentValue instanceof ConfigurationSection) {
                writer.write(indents + trailingKey + ":");
                if (!((ConfigurationSection) currentValue).getKeys(false).isEmpty())
                    writer.write("\n");
                else
                    writer.write(" {}\n");
                continue;
            }
            parserConfig.set(trailingKey, currentValue);
            String yaml = parserConfig.saveToString();
            yaml = yaml.substring(0, yaml.length() - 1).replace("\n", "\n" + indents);
            String toWrite = indents + yaml + "\n";
            parserConfig.set(trailingKey, null);
            writer.write(toWrite);
        }
        String danglingComments = comments.get(null);
        if (danglingComments != null)
            writer.write(danglingComments);
        writer.close();
    }
    private static Map<String, String> parseComments(Plugin plugin, String resourceName, FileConfiguration defaultConfig) throws IOException {
        List<String> keys = new ArrayList<>(defaultConfig.getKeys(true));
        BufferedReader reader = new BufferedReader(new InputStreamReader((plugin.getResource(resourceName)), StandardCharsets.UTF_8));
        Map<String, String> comments = new LinkedHashMap<>();
        StringBuilder commentBuilder = new StringBuilder();
        KeyBuilder keyBuilder = new KeyBuilder(defaultConfig, SEPARATOR);
        String previousKey = null;
        String nextValidKey = null;
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmedLine = line.trim();
            if (nextValidKey != null) {
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    commentBuilder.append(trimmedLine).append("\n");
                    continue;
                }
                if (trimmedLine.startsWith("-"))
                    continue;
                keyBuilder.parseLine(trimmedLine, false);
                if (!defaultConfig.contains(keyBuilder.toString())) {
                    keyBuilder.removeLastKey();
                    continue;
                }
                if (keyBuilder.toString().equals(nextValidKey))
                    nextValidKey = null;
                keyBuilder.removeLastKey();
            }
            if (trimmedLine.startsWith("-")) {
                int currentKeyIndex = keys.indexOf(previousKey);
                if (currentKeyIndex != keys.size() - 1)
                    nextValidKey = keys.get(currentKeyIndex + 1);

                continue;
            }
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                commentBuilder.append(trimmedLine).append("\n");
            } else {
                keyBuilder.parseLine(trimmedLine, true);
                String key = keyBuilder.toString();
                if (commentBuilder.length() > 0) {
                    comments.put(key, commentBuilder.toString());
                    commentBuilder.setLength(0);
                }
                previousKey = key;
                int nextKeyIndex = keys.indexOf(keyBuilder.toString()) + 1;
                if (nextKeyIndex < keys.size()) {
                    String nextKey = keys.get(nextKeyIndex);
                    while (!keyBuilder.isEmpty() && !nextKey.startsWith(keyBuilder.toString() + SEPARATOR)) {
                        keyBuilder.removeLastKey();
                    }
                }
            }
        }
        reader.close();
        if (commentBuilder.length() > 0)
            comments.put(null, commentBuilder.toString());
        return comments;
    }
    private static Map<String, String> parseIgnoredSections(File toUpdate, Map<String, String> comments, List<String> ignoredSections) throws IOException {
        Map<String, String> ignoredSectionValues = new LinkedHashMap<>(ignoredSections.size());
        DumperOptions options = new DumperOptions();
        options.setAllowUnicode(true);
        options.setLineBreak(DumperOptions.LineBreak.UNIX);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new YamlConstructor(), new YamlRepresenter(), options);
        Map<Object, Object> root = yaml.load(new FileReader(toUpdate));
        ignoredSections.forEach(section -> {
            String[] split = section.split("[" + SEPARATOR + "]");
            String key = split[split.length - 1];
            Map<Object, Object> map = getSection(section, root);
            StringBuilder keyBuilder = new StringBuilder();
            for (int i = 0; i < split.length; i++) {
                if (i != split.length - 1) {
                    if (keyBuilder.length() > 0)
                        keyBuilder.append(SEPARATOR);
                    keyBuilder.append(split[i]);
                }
            }
            ignoredSectionValues.put(section, buildIgnored(key, map, comments, keyBuilder, new StringBuilder(), yaml));
        });
        return ignoredSectionValues;
    }
    private static Map<Object, Object> getSection(String fullKey, Map<Object, Object> root) {
        String[] keys = fullKey.split("[" + SEPARATOR + "]", 2);
        String key = keys[0];
        Object value = root.get(getKeyAsObject(key, root));
        if (keys.length == 1) {
            if (value instanceof Map)
                return root;
            throw new IllegalArgumentException("Ignored sections must be a ConfigurationSection not a value!");
        }
        if (!(value instanceof Map))
            throw new IllegalArgumentException("Invalid ignored ConfigurationSection specified!");
        return getSection(keys[1], (Map<Object, Object>) value);
    }
    private static String buildIgnored(String fullKey, Map<Object, Object> ymlMap, Map<String, String> comments, StringBuilder keyBuilder, StringBuilder ignoredBuilder, Yaml yaml) {
        String[] keys = fullKey.split("[" + SEPARATOR + "]", 2);
        String key = keys[0];
        Object originalKey = getKeyAsObject(key, ymlMap);
        if (keyBuilder.length() > 0)
            keyBuilder.append(".");
        keyBuilder.append(key);
        if (!ymlMap.containsKey(originalKey)) {
            if (keys.length == 1)
                throw new IllegalArgumentException("Invalid ignored section: " + keyBuilder);
            throw new IllegalArgumentException("Invalid ignored section: " + keyBuilder + "." + keys[1]);
        }
        String comment = comments.get(keyBuilder.toString());
        String indents = KeyBuilder.getIndents(keyBuilder.toString(), SEPARATOR);
        if (comment != null)
            ignoredBuilder.append(addIndentation(comment, indents)).append("\n");
        ignoredBuilder.append(addIndentation(key, indents)).append(":");
        Object obj = ymlMap.get(originalKey);
        if (obj instanceof Map) {
            ignoredBuilder.append("\n");
            Map<Object, Object> map = (Map<Object, Object>) obj;
            StringBuilder preLoopKey = new StringBuilder(keyBuilder);
            for (Object o : map.keySet()) {
                buildIgnored(o.toString(), map, comments, keyBuilder, ignoredBuilder, yaml);
                keyBuilder = new StringBuilder(preLoopKey);
            }
        } else {
            writeIgnoredValue(yaml, obj, ignoredBuilder, indents);
        }
        return ignoredBuilder.toString();
    }
    private static void writeIgnoredValue(Yaml yaml, Object toWrite, StringBuilder ignoredBuilder, String indents) {
        String yml = yaml.dump(toWrite);
        if (toWrite instanceof Collection) {
            ignoredBuilder.append("\n").append(addIndentation(yml, indents)).append("\n");
        } else {
            ignoredBuilder.append(" ").append(yml);
        }
    }
    private static String addIndentation(String s, String indents) {
        StringBuilder builder = new StringBuilder();
        String[] split = s.split("\n");
        for (String value : split) {
            if (builder.length() > 0)
                builder.append("\n");
            builder.append(indents).append(value);
        }
        return builder.toString();
    }
    private static void writeCommentIfExists(Map<String, String> comments, BufferedWriter writer, String fullKey, String indents) throws IOException {
        String comment = comments.get(fullKey);
        if (comment != null)
            writer.write(indents + comment.substring(0, comment.length() - 1).replace("\n", "\n" + indents) + "\n");
    }
    private static Object getKeyAsObject(String key, Map<Object, Object> sectionContext) {
        if (sectionContext.containsKey(key))
            return key;
        try {
            Float keyFloat = Float.parseFloat(key);
            if (sectionContext.containsKey(keyFloat))
                return keyFloat;
        } catch (NumberFormatException ignored) {}
        try {
            Double keyDouble = Double.parseDouble(key);
            if (sectionContext.containsKey(keyDouble))
                return keyDouble;
        } catch (NumberFormatException ignored) {}
        try {
            Integer keyInteger = Integer.parseInt(key);
            if (sectionContext.containsKey(keyInteger))
                return keyInteger;
        } catch (NumberFormatException ignored) {}
        try {
            Long longKey = Long.parseLong(key);
            if (sectionContext.containsKey(longKey))
                return longKey;
        } catch (NumberFormatException ignored) {}

        return null;
    }
}