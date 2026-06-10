package com.xkits.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtil {

    private MessageUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String format(String message, Map<String, String> placeholders) {
        if (message == null) {
            return "";
        }
        String formatted = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return color(formatted);
    }

    public static String color(String message) {
        return message == null ? "" : message.replace('&', '§');
    }

    public static String format(String prefix, String message, Map<String, String> placeholders) {
        if (message == null) {
            return "";
        }
        String formatted = prefix + message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return color(formatted);
    }

    public static long parseDuration(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Invalid duration");
        }

        String normalized = input.trim().toLowerCase();
        long totalSeconds = 0;
        boolean matched = false;

        Pattern pattern = Pattern.compile("(\\d+)([dhms])");
        Matcher matcher = pattern.matcher(normalized);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() != lastEnd) {
                throw new IllegalArgumentException("Invalid duration format");
            }
            matched = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            totalSeconds += switch (unit) {
                case "d" -> value * 86_400;
                case "h" -> value * 3_600;
                case "m" -> value * 60;
                case "s" -> value;
                default -> throw new IllegalArgumentException("Invalid duration unit");
            };
            lastEnd = matcher.end();
        }

        if (!matched) {
            if (normalized.matches("\\d+")) {
                return Long.parseLong(normalized);
            }
            throw new IllegalArgumentException("Invalid duration format");
        }

        if (lastEnd != normalized.length()) {
            throw new IllegalArgumentException("Invalid duration format");
        }

        return totalSeconds;
    }

    public static String formatDuration(long totalSeconds) {
        if (totalSeconds <= 0) {
            return "0s";
        }

        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append("d");
        }
        if (hours > 0) {
            if (result.length() > 0) result.append(' ');
            result.append(hours).append("h");
        }
        if (minutes > 0) {
            if (result.length() > 0) result.append(' ');
            result.append(minutes).append("m");
        }
        if (seconds > 0 || result.length() == 0) {
            if (result.length() > 0) result.append(' ');
            result.append(seconds).append("s");
        }
        return result.toString();
    }
}
