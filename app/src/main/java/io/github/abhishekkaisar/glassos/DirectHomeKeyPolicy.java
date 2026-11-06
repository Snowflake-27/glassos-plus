package io.github.abhishekkaisar.glassos.personal;

/** Optional HOME-only input filtering. Other keys are never consumed. */
public final class DirectHomeKeyPolicy {
    private DirectHomeKeyPolicy() {}
    public static boolean accepts(boolean enabled, int keyCode) { return enabled && keyCode == 3; }
    public static int flags(int current, boolean enabled) {
        return enabled ? current | 0x20 : current & ~0x20;
    }
}
