package io.github.abhishekkaisar.glassos.personal

import org.json.JSONObject

/** Public movie/series metadata only: no account, API key, stream URLs or playback. */
object Cinemeta {
    private const val BASE = "https://v3-cinemeta.strem.io"
    fun fetch(content: String): List<HeroRepo.Item> {
        val types = when (content) { "movie" -> listOf("movie"); "series" -> listOf("series"); else -> listOf("movie", "series") }
        val lists = types.map { type ->
            val (code, body) = HeroRepo.get("$BASE/catalog/$type/top.json")
            if (code != 200 || body == null) return@map emptyList<HeroRepo.Item>()
            val metas = try { JSONObject(body).optJSONArray("metas") } catch (_: Exception) { null }
                ?: return@map emptyList<HeroRepo.Item>()
            (0 until metas.length()).mapNotNull { i ->
                val m = metas.optJSONObject(i) ?: return@mapNotNull null
                val id = m.optString("imdb_id").ifEmpty { m.optString("id") }
                val link = TitleLinks.build(TitleLinks.NUVIO, type, id) ?: return@mapNotNull null
                val background = m.optString("background").replace("https://images.metahub.space/background/medium/", "https://images.metahub.space/background/large/")
                if (!background.startsWith("https://")) return@mapNotNull null
                val genres = m.optJSONArray("genres")
                if (genres != null && (0 until genres.length()).any { genres.optString(it).equals("adult", true) }) return@mapNotNull null
                val logo = m.optString("logo").takeIf { it.startsWith("https://") }
                HeroRepo.Item("cinemeta_${type}_$id", m.optString("name"),
                    if (type == "movie") "Movie" else "Series", link, background, logo, TitleLinks.NUVIO)
            }.distinctBy { it.id }.shuffled().take(if (types.size == 1) 24 else 12)
        }
        // A fresh shuffled selection, with a bounded decoded-image cache in HeroView.
        return (0 until 24).flatMap { i -> lists.mapNotNull { it.getOrNull(i) } }.take(24)
    }
}
