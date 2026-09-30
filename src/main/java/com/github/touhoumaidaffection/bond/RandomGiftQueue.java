package com.github.touhoumaidaffection.bond;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Durable earned slots; prepared IDs are the FIFO prefix, never extra earned gifts. */
public record RandomGiftQueue(int queued, List<String> prepared) {
    public static final int MAX_QUEUED = 64;
    private static final java.util.regex.Pattern ITEM_ID = java.util.regex.Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");

    public RandomGiftQueue {
        queued = Math.max(0, Math.min(MAX_QUEUED, queued));
        List<String> valid = new ArrayList<>();
        for (String id : prepared) {
            if (valid.size() >= queued) break;
            if (id != null && id.length() <= 256 && ITEM_ID.matcher(id).matches()) {
                valid.add(id);
            }
        }
        prepared = List.copyOf(valid);
    }

    public RandomGiftQueue retain(Predicate<String> allowed) {
        List<String> retained = null;
        for (int i = 0; i < prepared.size(); i++) {
            String id = prepared.get(i);
            if (!allowed.test(id)) {
                if (retained == null) retained = new ArrayList<>(prepared.subList(0, i));
            } else if (retained != null) {
                retained.add(id);
            }
        }
        return retained == null ? this : new RandomGiftQueue(queued, retained);
    }

    public RandomGiftQueue consumeFirst() {
        if (prepared.isEmpty()) return this;
        return new RandomGiftQueue(queued - 1, prepared.subList(1, prepared.size()));
    }
}
