package io.github.abhishekkaisar.glassos.personal;

/** Screen-relative layout choices, independent of Android for local checks. */
public final class LayoutMath {
    private LayoutMath() {}
    /** Original GlassOS dock geometry; independent of any extra content rows. */
    public static int dockTop(int height, int tileHeight, int verticalPadding, int peek, int peekGap) {
        return height - peek - peekGap - verticalPadding - tileHeight;
    }
    /** Dock moves up only while the lower carousel is selected. */
    public static int homeTranslation(boolean continueFocused, int continueSpace) {
        return continueFocused ? -Math.max(0, continueSpace) : 0;
    }
    public static int continueHeight(int width, int tileWidth) {
        return tileWidth * 9 / 16 + width * 100 / 1920;
    }
    public static int columns(int value) { return Math.max(4, Math.min(6, value)); }
    public static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    public static int tileWidth(int width, int columns, int margin, int gap, int sizePercent) {
        int slot = (width - 2 * margin - (columns(columns) - 1) * gap) / columns(columns);
        return Math.max(1, slot * clamp(sizePercent, 80, 100) / 100);
    }
    public static int rowInset(int width, int count, int tileWidth, int gap, int minimum) {
        return Math.max(minimum, (width - count * tileWidth - Math.max(0, count - 1) * gap) / 2);
    }
}
