package io.github.abhishekkaisar.glassos.personal

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.ContentObserver
import android.media.tv.TvContract
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Executors

/** Only public TV-provider metadata. No app-private databases, privileged grants, or provider writes. */
data class ContinueItem(
    val id: String, val pkg: String, val app: String, val title: String,
    val season: String, val episode: String, val episodeTitle: String, val year: String,
    val art: String, val link: String, val position: Long, val duration: Long,
    val activity: Long, val weight: Long, val source: String
) {
    val identity get() = ContinueLogic.identity(title, season, episode, year)
    val nuvio get() = pkg == "com.nuvio.app" || pkg == "com.nuvio.tv"
    val fraction get() = ContinueLogic.progress(position, duration)
    fun json() = JSONObject().apply {
        put("id", id); put("pkg", pkg); put("app", app); put("title", title)
        put("season", season); put("episode", episode); put("episodeTitle", episodeTitle); put("year", year)
        put("art", art); put("link", link); put("position", position); put("duration", duration)
        put("activity", activity); put("weight", weight); put("source", source)
    }
    companion object {
        fun from(o: JSONObject) = ContinueItem(o.getString("id"), o.getString("pkg"), o.getString("app"), o.getString("title"),
            o.optString("season"), o.optString("episode"), o.optString("episodeTitle"), o.optString("year"),
            o.optString("art"), o.optString("link"), o.optLong("position", -1), o.optLong("duration", -1),
            o.optLong("activity", 0), o.optLong("weight", 0), o.optString("source"))
    }
}
data class ContinueState(val items: List<ContinueItem>, val message: String, val permission: Boolean)

@android.annotation.TargetApi(26)
class ContinueWatchingRepository(context: Context, private val deliver: (ContinueState) -> Unit) {
    private val ctx = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private var active = false
    private var closed = false
    private var registered = false
    private var loading = false
    private var pending = false
    private val file get() = File(ctx.filesDir, "continue-watching.json")
    private val change = Runnable { refresh() }
    private val observer = object : ContentObserver(main) {
        override fun onChange(selfChange: Boolean) { main.removeCallbacks(change); if (active) main.postDelayed(change, 800) }
    }
    fun permitted() = Build.VERSION.SDK_INT >= 26 && ctx.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED
    fun start() {
        if (closed) return
        active = true
        if (permitted() && !registered) {
            try {
                for (u in listOf(TvContract.WatchNextPrograms.CONTENT_URI, TvContract.PreviewPrograms.CONTENT_URI, TvContract.Channels.CONTENT_URI))
                    ctx.contentResolver.registerContentObserver(u, true, observer)
                registered = true
            } catch (_: Exception) { ctx.contentResolver.unregisterContentObserver(observer) }
        }
        refresh()
    }
    fun stop() {
        active = false; main.removeCallbacks(change)
        if (registered) { ctx.contentResolver.unregisterContentObserver(observer); registered = false }
    }
    fun release() { stop(); closed = true; main.removeCallbacksAndMessages(null); io.shutdownNow() }
    fun refresh() {
        if (!active || closed) return
        if (Build.VERSION.SDK_INT < 26) { deliver(ContinueState(emptyList(), "Requires Android TV 8 or newer", false)); return }
        if (!permitted()) {
            file.delete()
            deliver(ContinueState(emptyList(), "Press OK to allow access to TV watch history", false)); return
        }
        if (loading) { pending = true; return }
        loading = true
        io.execute {
            val state = try { readProvider() } catch (_: SecurityException) {
                file.delete(); ContinueState(emptyList(), "TV provider denied access. Check TV listings permission.", true)
            } catch (_: Exception) {
                val cached = readCache()
                ContinueState(cached, if (cached.isEmpty()) "TV provider unavailable. Press OK to retry." else "Saved items · provider temporarily unavailable", true)
            }
            main.post {
                loading = false
                if (!closed && active) deliver(state)
                if (pending) { pending = false; refresh() }
            }
        }
    }
    private data class Channel(val pkg: String, val name: String)
    private fun readProvider(): ContinueState {
        val channels = HashMap<Long, Channel>()
        var channelRows = 0; var watchRows = 0; var previewRows = 0
        val failures = ArrayList<String>()
        fun query(uri: Uri, handle: (Cursor) -> Unit) {
            // No SQL selection/sort: sideloaded readers must filter publicly searchable rows in code.
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                var n = 0
                while (c.moveToNext() && n++ < 2000) handle(c)
            } ?: throw IllegalStateException("TV provider returned no cursor")
        }
        try { query(TvContract.Channels.CONTENT_URI) { c ->
            channelRows++
            channels[c.num("_id")] = Channel(c.str("package_name"), c.str("display_name"))
        } } catch (e: Exception) { failures.add("channels:" + e.javaClass.simpleName) }
        val items = ArrayList<ContinueItem>()
        val counts = HashMap<String, Int>()
        fun row(c: Cursor, source: String) {
            val channel = channels[c.num("channel_id")]
            val pkg = c.str("package_name").ifBlank { channel?.pkg ?: "" }
            if (pkg.isBlank() || pkg == ctx.packageName) return
            counts[pkg] = (counts[pkg] ?: 0) + 1
            val title = c.str("title").trim()
            if (title.isBlank()) return
            val pos = c.num("last_playback_position_millis", -1)
            val duration = c.num("duration_millis", -1)
            if (!ContinueLogic.unfinished(pos, duration)) return
            val type = c.num("watch_next_type", -1)
            val resumeChannel = channel?.name?.lowercase()?.let { name ->
                listOf("continue watching", "continuar viendo", "seguir viendo", "in progress", "watch next", "play next", "resume watching").any { name.contains(it) }
            } ?: false
            val qualifies = if (source == "watch_next") type == 0L || type == 1L || (type == -1L && pos > 0)
                else resumeChannel || pos > 0
            if (!qualifies) return
            val label = try { ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString() } catch (_: Exception) { return }
            val link = c.str("intent_uri")
            val valid = resumeIntent(ctx, pkg, link) != null
            val art = c.str("thumbnail_uri").ifBlank { c.str("poster_art_uri") }
            items.add(ContinueItem(source + ":" + c.num("_id"), pkg, label, title,
                c.str("season_display_number").ifBlank { c.str("season_number") },
                c.str("episode_display_number").ifBlank { c.str("episode_number") }, c.str("episode_title"),
                c.str("release_date").take(4).takeIf { it.all(Char::isDigit) } ?: "",
                art, if (valid) link else "", pos, duration, c.num("last_engagement_time_utc_millis", 0),
                c.num("weight", 0), source))
        }
        try { query(TvContract.WatchNextPrograms.CONTENT_URI) { watchRows++; row(it, "watch_next") } }
        catch (e: Exception) { failures.add("watch_next:" + e.javaClass.simpleName) }
        try { query(TvContract.PreviewPrograms.CONTENT_URI) { previewRows++; row(it, "preview") } }
        catch (e: Exception) { failures.add("preview:" + e.javaClass.simpleName) }
        val order = compareByDescending<ContinueItem> { ContinueLogic.rank(it.activity, it.nuvio) }
            .thenByDescending { it.nuvio }.thenByDescending { it.link.isNotEmpty() }.thenByDescending { it.weight }
            .thenBy { it.identity }
        val merged = ArrayList<ContinueItem>()
        for (item in items.sortedWith(order)) {
            val baseKey = ContinueLogic.identity(item.title, item.season, item.episode, "")
            val at = merged.indexOfFirst { old ->
                ContinueLogic.identity(old.title, old.season, old.episode, "") == baseKey &&
                    (old.year == item.year || old.year.isBlank() || item.year.isBlank())
            }
            if (at < 0) merged.add(item)
            else if (merged[at].link.isBlank() && item.link.isNotBlank()) merged[at] = item
        }
        val sorted = merged.sortedWith(order).take(80)
        val diagnostic = JSONObject().apply {
            put("at", System.currentTimeMillis()); put("permission", permitted())
            put("channels", channelRows); put("watchNextRows", watchRows); put("previewRows", previewRows)
            put("eligible", items.size); put("merged", sorted.size); put("packages", JSONObject(counts as Map<*, *>))
            put("nuvioChannels", JSONArray(channels.filterValues { it.pkg == "com.nuvio.app" || it.pkg == "com.nuvio.tv" }.keys.toList()))
            put("failures", JSONArray(failures))
        }
        // Counts only, no titles/deep links in diagnostic logs. Cache remains in private app storage.
        File(ctx.filesDir, "continue-diagnostic.json").writeText(diagnostic.toString())
        if (failures.size >= 2 && sorted.isEmpty()) throw IllegalStateException("TV provider tables unavailable")
        writeCache(sorted)
        return ContinueState(sorted, when {
            failures.isNotEmpty() -> "Some TV channels are unavailable"
            sorted.isEmpty() -> "No apps are sharing in-progress titles yet. Open Nuvio and enable its TV channel."
            else -> ""
        }, true)
    }
    private fun writeCache(items: List<ContinueItem>) {
        val tmp = File(file.path + ".tmp")
        tmp.writeText(JSONObject().put("savedAt", System.currentTimeMillis()).put("items", JSONArray(items.map { it.json() })).toString())
        if (!tmp.renameTo(file)) tmp.delete()
    }
    private fun readCache(): List<ContinueItem> = try {
        if (!permitted() || !file.exists() || System.currentTimeMillis() - file.lastModified() > 86400000) emptyList()
        else JSONObject(file.readText()).getJSONArray("items").let { a -> (0 until a.length()).map { ContinueItem.from(a.getJSONObject(it)) } }
    } catch (_: Exception) { emptyList() }
    private fun Cursor.str(name: String): String = getColumnIndex(name).let { if (it < 0 || isNull(it)) "" else getString(it) ?: "" }
    private fun Cursor.num(name: String, fallback: Long = 0): Long = getColumnIndex(name).let { if (it < 0 || isNull(it)) fallback else try { getLong(it) } catch (_: Exception) { fallback } }
    companion object {
        const val PERMISSION = "android.permission.READ_TV_LISTINGS"
        fun resumeIntent(ctx: Context, pkg: String, raw: String): Intent? = try {
            if (raw.isBlank() || raw.length > 16000) null else {
                val i = Intent.parseUri(raw, Intent.URI_INTENT_SCHEME)
                if ((i.component != null && i.component!!.packageName != pkg) || (i.`package` != null && i.`package` != pkg)) null
                else if (i.data?.scheme in listOf("file", "javascript", "data")) null
                else {
                    i.selector = null; i.clipData = null
                    i.setPackage(pkg)
                    i.flags = (i.flags and (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)) or Intent.FLAG_ACTIVITY_NEW_TASK
                    val info = ctx.packageManager.resolveActivity(i, 0)?.activityInfo
                    if (info != null && info.exported && info.enabled && info.applicationInfo.enabled &&
                        (info.permission == null || ctx.checkSelfPermission(info.permission) == PackageManager.PERMISSION_GRANTED)) i else null
                }
            }
        } catch (_: Exception) { null }
    }
}
