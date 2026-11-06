package io.github.abhishekkaisar.glassos.personal

import org.json.JSONObject
import java.util.Random

/** Public metadata only. Bounded requests/artwork; no account, stream URLs or playback. */
object Cinemeta {
    private const val BASE = "https://v3-cinemeta.strem.io"
    const val MAX_TITLES = 120
    // Cinemeta currently returns 50 entries per page (the SDK's generic example uses 100).
    private val PAGES = listOf("top.json", "top/skip=50.json", "top/skip=100.json",
        "imdbRating.json", "imdbRating/skip=50.json")

    fun fetch(content: String, previous: Set<String> = emptySet()): List<HeroRepo.Item> {
        val types = when (content) { "movie" -> listOf("movie"); "series" -> listOf("series"); else -> listOf("movie", "series") }
        val lists = types.map { type ->
            val items = LinkedHashMap<String, HeroRepo.Item>()
            for (page in PAGES) {
                val (code, body) = HeroRepo.get("$BASE/catalog/$type/$page")
                if (code != 200 || body == null) continue
                val metas = try { JSONObject(body).optJSONArray("metas") } catch (_: Exception) { null } ?: continue
                for (i in 0 until metas.length()) {
                    val m = metas.optJSONObject(i) ?: continue
                    val id = m.optString("imdb_id").ifEmpty { m.optString("id") }
                    val link = TitleLinks.build(TitleLinks.NUVIO, type, id) ?: continue
                    val background = m.optString("background").replace("https://images.metahub.space/background/medium/", "https://images.metahub.space/background/large/")
                    if (!background.startsWith("https://") || m.optString("name").isBlank()) continue
                    val genres = m.optJSONArray("genres")
                    if (genres != null && (0 until genres.length()).any { genres.optString(it).equals("adult", true) }) continue
                    val logo = m.optString("logo").takeIf { it.startsWith("https://") }
                    val key = "cinemeta_${type}_$id"
                    if (!items.containsKey(key)) items[key] = HeroRepo.Item(key, m.optString("name"),
                        if (type == "movie") "Movie" else "Series", link, background, logo, TitleLinks.NUVIO)
                }
            }
            val keys = HeroCatalogSelection.choose(items.keys.toList(), previous, MAX_TITLES / types.size, Random())
            keys.mapNotNull { items[it] }
        }
        // Interleave both types without duplicating identifiers. Failed pages leave successful pages usable.
        return (0 until MAX_TITLES).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } }.take(MAX_TITLES)
    }
}
