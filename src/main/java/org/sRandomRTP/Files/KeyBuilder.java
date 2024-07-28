package org.sRandomRTP.Files;

import org.bukkit.configuration.file.FileConfiguration;

public class KeyBuilder implements Cloneable {
    private final FileConfiguration config;
    private final char separator;
    private final StringBuilder builder;
    public KeyBuilder(FileConfiguration config, char separator) {
        this.config = config;
        this.separator = separator;
        this.builder = new StringBuilder();
    }
    private KeyBuilder(KeyBuilder keyBuilder) {
        this.config = keyBuilder.config;
        this.separator = keyBuilder.separator;
        this.builder = new StringBuilder(keyBuilder.toString());
    }
    public void parseLine(String line, boolean checkIfExists) {
        line = line.trim();
        String[] currentSplitLine = line.split(":");
        if (currentSplitLine.length > 2)
            currentSplitLine = line.split(": ");
        String key = currentSplitLine[0].replace("'", "").replace("\"", "");
        if (checkIfExists) {
            while (builder.length() > 0 && !config.contains(builder.toString() + separator + key)) {
                removeLastKey();
            }
        }
        if (builder.length() > 0)
            builder.append(separator);
        builder.append(key);
    }
    public boolean isEmpty() {
        return builder.length() == 0;
    }
    public static boolean isSubKeyOf(String parentKey, String subKey, char separator) {
        if (parentKey.isEmpty())
            return false;
        return subKey.startsWith(parentKey)
                && subKey.substring(parentKey.length()).startsWith(String.valueOf(separator));
    }
    public static String getIndents(String key, char separator) {
        String[] splitKey = key.split("[" + separator + "]");
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < splitKey.length; i++) {
            builder.append("  ");
        }
        return builder.toString();
    }
    public void removeLastKey() {
        if (builder.length() == 0)
            return;
        String keyString = builder.toString();
        String[] split = keyString.split("[" + separator + "]");
        int minIndex = Math.max(0, builder.length() - split[split.length - 1].length() - 1);
        builder.replace(minIndex, builder.length(), "");
    }
    @Override
    public String toString() {
        return builder.toString();
    }
    @Override
    protected KeyBuilder clone() {
        return new KeyBuilder(this);
    }
}