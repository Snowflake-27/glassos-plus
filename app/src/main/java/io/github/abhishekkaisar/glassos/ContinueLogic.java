package io.github.abhishekkaisar.glassos.personal;

import java.text.Normalizer;
import java.util.Locale;

/** Provider-independent rules; unknown progress/activity stays unknown. */
public final class ContinueLogic {
    private ContinueLogic() {}
    public static String identity(String title, String season, String episode, String year) {
        String t = Normalizer.normalize(title, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        return t + "|" + season + "|" + episode + "|" + year;
    }
    public static float progress(long position, long duration) {
        return position < 0 || duration <= 0 ? -1f : Math.min(1f, position / (float) duration);
    }
    public static boolean unfinished(long position, long duration) {
        return duration <= 0 || position < duration;
    }
    public static long rank(long activity, boolean nuvio) {
        if (activity <= 0) return 0;
        return activity + (nuvio ? 120000L : 0L);
    }
    public static boolean mayAdvance(boolean active, boolean focused, int count, long idleMillis) {
        return active && !focused && count > 1 && idleMillis >= 9000L;
    }
    public static int wrap(int index, int count) { return count <= 0 ? 0 : ((index % count) + count) % count; }
}
