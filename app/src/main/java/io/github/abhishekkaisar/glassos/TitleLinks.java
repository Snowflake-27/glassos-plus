package io.github.abhishekkaisar.glassos.personal;

/** Only exact IMDb identifiers are routed; never guess a movie from a similar name. */
public final class TitleLinks {
    private TitleLinks() {}
    public static final String NUVIO = "com.nuvio.app";
    public static final String STREMIO = "com.stremio.one";
    public static String build(String destination, String type, String imdb) {
        if (imdb == null || !imdb.matches("tt[0-9]{7,12}")) return null;
        if (!"movie".equals(type) && !"series".equals(type)) return null;
        if (NUVIO.equals(destination)) return "nuvio://" + type + "/" + imdb;
        if (STREMIO.equals(destination)) return "stremio:///detail/" + type + "/" + imdb + "/" + imdb;
        return null;
    }
    public static String fromKey(String destination, String key) {
        if (key == null) return null;
        String[] parts = key.replaceFirst("^t:", "").split("_", 3);
        if (parts.length != 3 || !"cinemeta".equals(parts[0])) return null;
        return build(destination, parts[1], parts[2]);
    }
}
