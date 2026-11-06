package io.github.abhishekkaisar.glassos.personal;

import java.util.*;

/** A bounded shuffle bag of identifiers; current title is excluded at every draw. */
public final class HeroShuffleBag {
    private final Random random;
    private final List<String> remaining = new ArrayList<>();
    private Set<String> catalog = Collections.emptySet();
    public HeroShuffleBag() { this(new Random()); }
    public HeroShuffleBag(Random random) { this.random = random; }
    public String draw(List<String> keys, String current) {
        Set<String> unique = new LinkedHashSet<>(keys);
        unique.remove(null);
        unique.remove("");
        if (!unique.equals(catalog)) { remaining.clear(); catalog = unique; }
        remaining.remove(current);
        if (remaining.isEmpty()) {
            remaining.addAll(unique);
            remaining.remove(current);
            Collections.shuffle(remaining, random);
        }
        return remaining.isEmpty() ? null : remaining.remove(remaining.size() - 1);
    }
}
