package io.github.abhishekkaisar.glassos.personal;

import java.util.Set;

/** Only exact stock HOME activities may trigger the accessibility redirect. */
public final class HomeRedirectPolicy {
    private HomeRedirectPolicy() {}
    public static boolean accepts(boolean enabled, boolean allowed, String pkg, String cls, Set<String> homes) {
        return enabled && allowed && pkg != null && cls != null && homes.contains(pkg + "/" + cls);
    }
}
