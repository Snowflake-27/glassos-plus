# Hero selection and title links

## Before this release

`Cinemeta.fetch` downloads the public Cinemeta `movie/top.json` and `series/top.json` catalogs. It excludes adult genres and invalid IMDb/artwork entries, removes duplicate identifiers, shuffles each list and selects up to 12 from each (24 total). A movie-only or series-only selection takes up to 24. Artwork uses the provider URL, with Metahub medium backgrounds upgraded to large. This is a public catalog, not a personalized recommendation engine.

`HeroRepo` caches complete title metadata and downloaded art under private `files/hero/<source>`. Cinemeta is the default source. Optional TMDB uses the user's own API key and selects up to eight trending titles; the optional unofficial Apple TV catalog also selects up to eight. Built-in scenes and personal photos remain available.

`HomeScreen.refetchTopShelf()` invalidated the source cache and called `setWallpaper`, which reused cached entries while fetching replacements. It did not simply choose the next title. `HeroView.setSlides` retained the visible title if present in the new list, so pressing the old Shuffle setting could appear to do nothing.

Each `Slide` includes its key/identifier, artwork, title, subtitle, logo and link metadata. A transition changes that whole slide; there is no independent mismatched background/title selection. A long plot description is not part of the current Cinemeta subtitle UI.

`HeroView.doFrame` automatically advances sequentially through the selected list, holding for 8 seconds and crossfading for 1.1 seconds. It repeats after a complete cycle. Rotation pauses when focused, away from Home, behind overlays or in the background. Cache freshness is checked when Home resumes or the source is selected: metadata expires after six hours, failed requests retry after 30 minutes. There is no separate periodic six-hour background fetch while idle.

Catalog metadata survives restart; the visible selection previously did not. Before the catalog expansion, up to 24 Cinemeta (eight TMDB/Apple) title records were retained for the chosen source. `HeroView.trim` bounds its decoded-image cache to three entries; active crossfade, warming and worker references may transiently retain additional images. These are different limits.

## Hero refresh button

The icon-only native `HeroRefreshButton` sits immediately beside the Open capsule. Right from Open focuses it, Left returns to Open, Down returns to Apps and Up reaches the profile pill. OK calls `HeroView.shuffle`, which reuses the existing asynchronous image loader and 1.1-second crossfade. It does not rebuild Home or fetch a new catalog. The old artwork stays until decoding completes; rapid activations during loading/fading are ignored.

`HeroShuffleBag` draws unique cached identifiers without selecting the currently visible title. It visits the remaining bag before replenishing it. Empty/singleton catalogs do nothing. Automatic rotation and manual left/right browsing retain their existing sequence. Settings retains the separate, accurately named **Refresh hero catalog** action for a network catalog refresh.

The last committed visible slide key is now persisted by `Prefs.heroSelectedKey` and restored when that key remains available. A removed title falls back to the first valid slide. In-flight fades are not saved as partially visible states.

## Player protocols

`TitleLinks` accepts only exact IMDb identifiers and movie/series types:

| Player | Package | Movie | Series |
| --- | --- | --- | --- |
| Nuvio | `com.nuvio.app` | `nuvio://movie/tt0371746` | `nuvio://series/tt0944947` |
| Stremio | `com.stremio.one` | `stremio:///detail/movie/tt0371746/tt0371746` | `stremio:///detail/series/tt0944947/tt0944947` |
| WuPlay | `app.wuplay.androidtv` | `wuplay://movie/tt0371746` | `wuplay://series/tt0944947` |

WuPlay routes/package are documented by [TVRelay](https://github.com/hmartinez94/TVRelay) (`PlayerApp`), and its official Android TV distribution is [wuplayapp/wuplay-releases](https://github.com/wuplayapp/wuplay-releases). This fork implements the protocol independently; it includes no TVRelay implementation code. TVRelay's noncommercial license is not used for GlassOS+ source.

A direct WuPlay TMDB-ID format or episode route has not been established. TMDB source titles use the existing `Tmdb.titleLink` IMDb external-ID lookup instead. Hero series open the series detail; no episode identifier is fabricated. Continue Watching keeps the publishing app's original resume intent, including its episode extras.

Settings cycles Nuvio → Stremio → WuPlay. `HomeScreen.openSlide` checks installation, resolves a package-scoped ACTION_VIEW intent, and catches launch failures. It never falls back to a player's generic home screen. Missing apps/unsupported links show a message. Package queries declare all three players. An absent WuPlay installation permits negative-case testing only; protocol support must still be tested with the installed WuPlay build.

## Expanded Cinemeta catalog (1.1.1)

The cached selection now holds up to 120 valid titles, 60 movies plus 60 series in mixed mode or up to 120 of one type. Each type reads Popular pages at offsets 0/50/100 and Featured (`imdbRating`) pages at 0/50. These are public catalogs declared in the [Cinemeta manifest](https://v3-cinemeta.strem.io/manifest.json). Endpoints were checked against actual responses; the provider currently uses 50-entry pages.

Selection deduplicates exact type/IMDb IDs, shuffles candidates and prioritizes entries absent from the preceding cached selection. Older entries fill shortages only when needed. It is not a lifetime viewing-history exclusion. Failed pages leave other successful pages usable; a complete refresh failure preserves the existing disk catalog.

The six-hour refresh policy, artwork resolution, 8-second hold and 1.1-second fade remain unchanged. At 120 titles, an uninterrupted cycle is approximately 18 minutes, rather than 3–4. Actual length depends on successful artwork downloads. All selected artwork is cached on disk for offline use, so the first refresh downloads more data; the decoded cache remains limited to three entries. Cache replacement removes obsolete artwork. Settings → Refresh hero catalog can fetch the expanded selection immediately.
