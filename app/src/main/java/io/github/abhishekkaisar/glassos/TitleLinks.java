package io.github.abhishekkaisar.glassos.personal;

/** Only exact IMDb identifiers are routed; never guess a movie from a similar name. */
public final class TitleLinks {
    private TitleLinks() {}
    public static final String NUVIO = "com.nuvio.app";
    public static final String NUVIO_TV = "com.nuvio.tv";
    public static boolean isNuvio(String pkg) {
        return NUVIO.equals(pkg) || NUVIO_TV.equals(pkg) || "com.nuvio.appdebug".equals(pkg) || "com.nuviodebug.com".equals(pkg);
    }
    public static String[] packages(String destination) {
        return isNuvio(destination) ? new String[]{NUVIO, NUVIO_TV, "com.nuvio.appdebug", "com.nuviodebug.com"} : new String[]{destination};
    }
    /** Preserve the proven title-navigation workaround across official Nuvio distributions. */
    public static boolean needsTaskReset(String pkg) { return isNuvio(pkg); }
    public static final String STREMIO = "com.stremio.one";
    public static final String WUPLAY = "app.wuplay.androidtv";
    public static String label(String destination) {
        return STREMIO.equals(destination) ? "Stremio" : WUPLAY.equals(destination) ? "WuPlay" : "Nuvio";
    }
    public static String next(String destination) {
        return isNuvio(destination) ? STREMIO : STREMIO.equals(destination) ? WUPLAY : NUVIO;
    }
    public static String build(String destination, String type, String imdb) {
        if (imdb == null || !imdb.matches("tt[0-9]{7,12}")) return null;
        if (!"movie".equals(type) && !"series".equals(type)) return null;
        if (isNuvio(destination)) return "nuvio://" + type + "/" + imdb;
        if (STREMIO.equals(destination)) return "stremio:///detail/" + type + "/" + imdb + "/" + imdb;
        if (WUPLAY.equals(destination)) return "wuplay://" + type + "/" + imdb;
        return null;
    }
    public static String fromKey(String destination, String key) {
        if (key == null) return null;
        String[] parts = key.replaceFirst("^t:", "").split("_", 3);
        if (parts.length != 3 || !"cinemeta".equals(parts[0])) return null;
        return build(destination, parts[1], parts[2]);
    }
}
