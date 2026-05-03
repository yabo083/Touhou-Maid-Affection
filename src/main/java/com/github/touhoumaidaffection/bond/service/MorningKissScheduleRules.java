package com.github.touhoumaidaffection.bond.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MorningKissScheduleRules {
    private MorningKissScheduleRules() {
    }

    public static List<TimeRange> resolveAllowedTimeRanges(List<? extends String> rawRanges) {
        List<TimeRange> parsed = new ArrayList<>();
        for (String raw : rawRanges) {
            TimeRange range = TimeRange.parse(raw);
            if (range != null) {
                parsed.add(range);
            }
        }
        if (!parsed.isEmpty()) {
            return parsed;
        }
        return List.of(
                new TimeRange(0, 2000, DialoguePool.MORNING),
                new TimeRange(12000, 14000, DialoguePool.EVENING)
        );
    }

    public static int safeMinKissCount(int configuredMin, int configuredMax) {
        return Math.max(1, Math.min(configuredMin, configuredMax));
    }

    public static int safeMaxKissCount(int configuredMin, int configuredMax) {
        return Math.max(Math.max(1, configuredMin), configuredMax);
    }

    public enum DialoguePool {
        MORNING,
        EVENING,
        GENERAL
    }

    record TimeRange(int startTick, int endTick, DialoguePool dialoguePool) {
        static TimeRange parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String normalized = raw.trim();
            DialoguePool dialoguePool = DialoguePool.GENERAL;
            int bucketSeparator = normalized.indexOf('@');
            if (bucketSeparator > 0) {
                dialoguePool = parseDialoguePool(normalized.substring(0, bucketSeparator).trim());
                normalized = normalized.substring(bucketSeparator + 1).trim();
            }

            String[] parts = normalized.split("-", 2);
            if (parts.length != 2) {
                return null;
            }
            try {
                int start = parseTimeToken(parts[0].trim());
                int end = parseTimeToken(parts[1].trim());
                if (bucketSeparator <= 0) {
                    dialoguePool = inferDialoguePool(start, end);
                }
                return new TimeRange(start, end, dialoguePool);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }

        boolean contains(long tick) {
            int normalizedTick = normalizeTick((int) tick);
            if (startTick <= endTick) {
                return normalizedTick >= startTick && normalizedTick <= endTick;
            }
            return normalizedTick >= startTick || normalizedTick <= endTick;
        }

        boolean crossesMidnight() {
            return startTick > endTick;
        }

        String toDisplayString() {
            return formatTick(startTick) + "-" + formatTick(endTick);
        }

        private static int normalizeTick(int tick) {
            int normalized = tick % 24000;
            return normalized < 0 ? normalized + 24000 : normalized;
        }

        private static int parseTimeToken(String raw) {
            if (raw.contains(":")) {
                String[] parts = raw.split(":", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid time token");
                }
                int hour = Integer.parseInt(parts[0].trim());
                int minute = Integer.parseInt(parts[1].trim());
                if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                    throw new IllegalArgumentException("Time out of range");
                }
                int totalMinutes = hour * 60 + minute;
                int tick = (totalMinutes * 1000) / 60 - 6000;
                return normalizeTick(tick);
            }
            return normalizeTick(Integer.parseInt(raw));
        }

        private static String formatTick(int tick) {
            int normalized = normalizeTick(tick);
            int totalMinutes = (int) Math.floor(((normalized + 6000) % 24000) / 1000.0 * 60.0);
            int hour = totalMinutes / 60;
            int minute = totalMinutes % 60;
            return String.format("%02d:%02d", hour, minute);
        }

        private static DialoguePool parseDialoguePool(String raw) {
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "morning", "am", "早安", "morning_kiss" -> DialoguePool.MORNING;
                case "evening", "night", "pm", "晚安", "evening_kiss" -> DialoguePool.EVENING;
                default -> DialoguePool.GENERAL;
            };
        }

        private static DialoguePool inferDialoguePool(int startTick, int endTick) {
            int startMinutes = toClockMinutes(startTick);
            int endMinutes = toClockMinutes(endTick);
            int midpoint;
            if (startTick <= endTick) {
                midpoint = (startMinutes + endMinutes) / 2;
            } else {
                int adjustedEnd = endMinutes + 24 * 60;
                midpoint = ((startMinutes + adjustedEnd) / 2) % (24 * 60);
            }
            int hour = midpoint / 60;
            if (hour >= 5 && hour < 12) {
                return DialoguePool.MORNING;
            }
            if (hour >= 17 || hour < 3) {
                return DialoguePool.EVENING;
            }
            return DialoguePool.GENERAL;
        }

        private static int toClockMinutes(int tick) {
            int normalized = normalizeTick(tick);
            return (int) Math.floor(((normalized + 6000) % 24000) / 1000.0 * 60.0);
        }
    }
}
