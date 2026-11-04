package io.github.abhishekkaisar.glassos.personal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

/** Bounded, lazy artwork cache. Only fetches URIs actually published with visible TV-provider items. */
class ContinueArtwork(context: Context, private val changed: () -> Unit) {
    private val ctx = context.applicationContext
    private val bitmaps = LruCache<String, Bitmap>(12)
    private val pending = HashSet<String>()
    private val failed = HashMap<String, Long>()
    private val io = Executors.newFixedThreadPool(2)
    private val main = Handler(Looper.getMainLooper())
    private var closed = false
    private val directory = File(ctx.cacheDir, "continue-art").apply { mkdirs() }
    fun get(uri: String, width: Int, height: Int): Bitmap? {
        if (uri.isBlank() || closed) return null
        val key = "$uri|$width|$height"
        bitmaps.get(key)?.let { return it }
        if (key in pending || System.currentTimeMillis() - (failed[key] ?: 0) < 30000) return null
        pending.add(key)
        io.execute {
            val bitmap = try { load(uri, width, height) } catch (_: Exception) { null }
            main.post {
                pending.remove(key)
                if (!closed) {
                    if (bitmap != null) bitmaps.put(key, bitmap) else failed[key] = System.currentTimeMillis()
                    changed()
                }
            }
        }
        return null
    }
    private fun load(raw: String, w: Int, h: Int): Bitmap? {
        val uri = Uri.parse(raw)
        if (uri.scheme !in listOf("https", "http", "content", "android.resource")) return null
        val hash = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
        val disk = File(directory, hash)
        val bytes = if (disk.exists() && System.currentTimeMillis() - disk.lastModified() < 86400000) disk.readBytes() else {
            val data = if (uri.scheme == "http" || uri.scheme == "https") {
                val connection = URL(raw).openConnection() as HttpURLConnection
                connection.connectTimeout = 7000; connection.readTimeout = 7000
                try {
                    if (connection.responseCode != 200) return null
                    connection.inputStream.use { it.readBytesBounded() }
                } finally { connection.disconnect() }
            } else ctx.contentResolver.openInputStream(uri)?.use { it.readBytesBounded() } ?: return null
            if (data.size > LIMIT) return null
            synchronized(directory) {
                val tmp = File(directory, hash + ".tmp")
                tmp.writeBytes(data)
                tmp.renameTo(disk)
                var total = directory.listFiles()?.sumOf { it.length() } ?: 0L
                directory.listFiles()?.sortedBy { it.lastModified() }?.forEach {
                    if (total > 24000000 && it != disk) { total -= it.length(); it.delete() }
                }
            }
            data
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) { disk.delete(); return null }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= w && bounds.outHeight / (sample * 2) >= h) sample *= 2
        while (bounds.outWidth.toLong() / sample * (bounds.outHeight / sample) > 2000000L) sample *= 2
        val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val scale = maxOf(w / src.width.toFloat(), h / src.height.toFloat())
        val cropW = (w / scale).toInt().coerceIn(1, src.width)
        val cropH = (h / scale).toInt().coerceIn(1, src.height)
        val x = (src.width - cropW) / 2; val y = (src.height - cropH) / 2
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, Rect(x, y, x + cropW, y + cropH), Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        src.recycle()
        return out
    }
    private fun java.io.InputStream.readBytesBounded(): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (out.size() <= LIMIT) {
            val n = read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
    fun release() { closed = true; io.shutdownNow(); main.removeCallbacksAndMessages(null); bitmaps.evictAll() }
    companion object { private const val LIMIT = 4 * 1024 * 1024 }
}
