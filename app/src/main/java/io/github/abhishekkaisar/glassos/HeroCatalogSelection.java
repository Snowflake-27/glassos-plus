package io.github.abhishekkaisar.glassos.personal;

import java.util.*;

/** Unique bounded selection; prefer titles outside the preceding cached batch. */
public final class HeroCatalogSelection {
    private HeroCatalogSelection() {}
    public static List<String> choose(List<String> candidates, Set<String> previous, int limit, Random random) {
        List<String> fresh = new ArrayList<>(), repeated = new ArrayList<>();
        for (String key : new LinkedHashSet<>(candidates)) {
            if (key == null || key.isEmpty()) continue;
            (previous.contains(key) ? repeated : fresh).add(key);
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(repeated, random);
        fresh.addAll(repeated);
        return new ArrayList<>(fresh.subList(0, Math.min(Math.max(0, limit), fresh.size())));
    }
}
