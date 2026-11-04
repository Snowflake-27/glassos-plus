import io.github.abhishekkaisar.glassos.personal.LayoutMath;
import io.github.abhishekkaisar.glassos.personal.TitleLinks;
import io.github.abhishekkaisar.glassos.personal.HomeRedirectPolicy;
import java.util.Collections;
import java.util.Set;
import io.github.abhishekkaisar.glassos.personal.ContinueLogic;

/** Runs with the existing JDK; no Android device or new test dependency required. */
public final class PersonalizationCheck {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        Set<String> homes = Collections.singleton("stock/stock.HomeActivity");
        require(HomeRedirectPolicy.accepts(true, true, "stock", "stock.HomeActivity", homes), "Home ignored");
        require(!HomeRedirectPolicy.accepts(true, true, "stock", "stock.SettingsActivity", homes), "Settings hijacked");
        require(!HomeRedirectPolicy.accepts(true, true, "com.nuvio.app", "stock.HomeActivity", homes), "Streaming app hijacked");
        require(!HomeRedirectPolicy.accepts(false, true, "stock", "stock.HomeActivity", homes), "Disabled preference ignored");
        require(!HomeRedirectPolicy.accepts(true, false, "stock", "stock.HomeActivity", homes), "Explicit stock visit intercepted");
        require(!HomeRedirectPolicy.accepts(true, true, null, null, homes), "Null event accepted");
        require(ContinueLogic.progress(-1, 60000) == -1, "Unknown progress fabricated");
        require(ContinueLogic.progress(30000, 60000) == .5f, "Progress ratio wrong");
        require(ContinueLogic.progress(10000, 0) == -1, "Unknown runtime fabricated");
        require(!ContinueLogic.unfinished(60000, 60000), "Completed item retained");
        require(ContinueLogic.rank(0, true) == 0, "Unknown activity fabricated");
        require(ContinueLogic.rank(1000000, true) < ContinueLogic.rank(2000000, false), "Nuvio overrides much newer activity");
        require(ContinueLogic.identity("CAFÉ: Film", "1", "2", "2024").equals(ContinueLogic.identity("cafe film", "1", "2", "2024")), "Title dedup normalization");
        require(!ContinueLogic.identity("Show", "1", "2", "").equals(ContinueLogic.identity("Show", "1", "3", "")), "Different episodes merged");
        require(ContinueLogic.wrap(-1, 3) == 2 && ContinueLogic.wrap(3, 3) == 0, "Carousel wrap wrong");
        require(!ContinueLogic.mayAdvance(true, true, 4, 15000), "Focused row auto-advanced");
        require(!ContinueLogic.mayAdvance(true, false, 4, 8000), "Idle pause too short");
        require(ContinueLogic.mayAdvance(true, false, 4, 9000), "Idle resume failed");
        require(!ContinueLogic.mayAdvance(false, false, 4, 15000), "Paused home auto-advanced");
        require(LayoutMath.dockTop(1080, 169, 40, 50, 22) == 799, "Original Hero dock moved");
        require(LayoutMath.continueHeight(1920, 283) == 259, "Compact row geometry changed");
        int layouts = 0;
        for (int width : new int[]{960, 1280, 1920, 3840}) {
            for (int columns = 4; columns <= 6; columns++) {
                for (int edge : new int[]{24, 48, 64, 80, 104, 128}) {
                    for (int gapReference : new int[]{24, 40, 52, 64}) {
                        for (int size : new int[]{80, 90, 100}) {
                            int margin = width * edge / 1920 + width * 38 / 1920;
                            int gap = width * gapReference / 1920;
                            int tile = LayoutMath.tileWidth(width, columns, margin, gap, size);
                            int screenHeight = width * 9 / 16;
                            int tileHeight = tile * 3 / 5;
                            int pad = screenHeight * 40 / 1080;
                            int peekHeight = tileHeight * 30 / 100;
                            int peekGapHeight = screenHeight * 22 / 1080;
                            int originalDock = LayoutMath.dockTop(screenHeight, tileHeight, pad, peekHeight, peekGapHeight);
                            int continueHeight = LayoutMath.continueHeight(width, tile);
                            int continueTop = originalDock + tileHeight + pad + peekGapHeight;
                            require(continueTop >= screenHeight - peekHeight, "Continue occupies expanded Hero space");
                            require(continueTop + continueHeight - continueHeight <= screenHeight, "Compact row below viewport");
                            require(originalDock - continueHeight > screenHeight / 4, "Compact rows crowd Hero title");
                            require(tile > 0, "Non-positive tile");
                            for (int count = 1; count <= columns; count++) {
                                int left = LayoutMath.rowInset(width, count, tile, gap, margin);
                                int right = left + count * tile + (count - 1) * gap;
                                require(left >= margin && right <= width - margin, "Row leaves safe margin");
                                require(left - tile * .1 >= 0 && right + tile * .1 <= width, "Focused tile clipped");
                            }
                            layouts++;
                        }
                    }
                }
            }
        }
        require(LayoutMath.columns(1) == 4 && LayoutMath.columns(20) == 6, "Column range");
        require("nuvio://movie/tt1375666".equals(TitleLinks.fromKey(TitleLinks.NUVIO, "t:cinemeta_movie_tt1375666")), "Wrong movie link");
        require("nuvio://series/tt0903747".equals(TitleLinks.fromKey(TitleLinks.NUVIO, "t:cinemeta_series_tt0903747")), "Wrong series link");
        require("stremio:///detail/movie/tt1375666/tt1375666".equals(TitleLinks.build(TitleLinks.STREMIO, "movie", "tt1375666")), "Wrong Stremio link");
        require(TitleLinks.fromKey(TitleLinks.NUVIO, "t:tmdb_movie_27205") == null, "TMDB id guessed as IMDb");
        require(TitleLinks.build(TitleLinks.NUVIO, "movie", "tt1375666/../../other") == null, "Malformed id accepted");
        require(TitleLinks.build(TitleLinks.NUVIO, "other", "tt1375666") == null, "Unsupported type accepted");
        require(TitleLinks.build("com.netflix.ninja", "movie", "tt1375666") == null, "Wrong app fallback");
        System.out.println("Passed: " + layouts + " layout combinations and exact movie/series link checks.");
    }
}
