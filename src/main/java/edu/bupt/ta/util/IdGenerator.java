package edu.bupt.ta.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IdGenerator {

    private static final Pattern NUM_PATTERN = Pattern.compile("(\\d+)$");

    private IdGenerator() {
    }

    public static String next(String prefix, Collection<String> existingIds, int width) {
        int max = existingIds.stream()
                .filter(id -> id != null && id.startsWith(prefix))
                .map(IdGenerator::extractNumber)
                .max(Comparator.naturalOrder())
                .orElse(0);
        return prefix + String.format("%0" + width + "d", max + 1);
    }

    private static int extractNumber(String id) {
        Matcher matcher = NUM_PATTERN.matcher(id);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
}
