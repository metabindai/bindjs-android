package ai.metabind.bindjs.composables.ext

import android.content.Context
import android.graphics.Typeface
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Downloads, caches and registers the fonts named by `.font(CustomFont({ url: ... }))`,
 * mirroring `FontCache` in bindjs-apple.
 *
 * Process-wide rather than per-view, because a font URL is typically written on every
 * row of a list: the first view to ask starts one download, and every other view awaits
 * that same one. A face already in [loaded] resolves synchronously, so a re-render never
 * flashes back to the system font.
 *
 * The host app supplies the `INTERNET` permission, as it already does for the remote
 * images this renderer loads through Coil.
 */
internal object RemoteFontCache {

    private const val TAG = "RemoteFontCache"
    private const val TIMEOUT_MS = 15_000

    /** Source URL → the registered face. */
    private val loaded = HashMap<String, Typeface>()

    /** Source URL → its in-flight download, so the same URL is only fetched once. */
    private val inFlight = HashMap<String, Deferred<Typeface?>>()

    private val lock = Mutex()

    /**
     * Downloads outlive the view that asked for them. A composable leaving composition
     * cancels its own coroutine, and a download shared by several views must not die
     * with whichever one happens to scroll away first.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The face for [url] if it is already loaded, without suspending. */
    fun cached(url: String): Typeface? = synchronized(loaded) { loaded[url] }

    /** The face for [url], downloading and registering it if this is the first ask. */
    suspend fun typeface(context: Context, url: String): Typeface? {
        cached(url)?.let { return it }

        val application = context.applicationContext
        val download = lock.withLock {
            inFlight[url] ?: scope.async { load(application, url) }.also { inFlight[url] = it }
        }

        val typeface = runCatching { download.await() }.getOrNull()
        lock.withLock { if (inFlight[url] === download) inFlight.remove(url) }
        return typeface
    }

    private fun load(context: Context, url: String): Typeface? {
        val file = File(fontDirectory(context), cacheFileName(url))

        if (!file.exists() && !download(url, file)) return null

        // A face that will not parse is cached poison: delete it so the next render
        // re-downloads rather than failing forever on a truncated file.
        return runCatching { Typeface.createFromFile(file) }
            .onSuccess { synchronized(loaded) { loaded[url] = it } }
            .onFailure {
                Log.w(TAG, "Could not read font from $url", it)
                file.delete()
            }
            .getOrNull()
    }

    private fun download(url: String, destination: File): Boolean {
        // Written aside and renamed so an interrupted download cannot leave a partial
        // file sitting at the cache path, where it would look like a complete one.
        val partial = File(destination.parentFile, destination.name + ".partial")
        return try {
            (URL(url).openConnection() as HttpURLConnection).run {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                try {
                    if (responseCode !in 200..299) {
                        Log.w(TAG, "Font request for $url returned HTTP $responseCode")
                        return false
                    }
                    inputStream.use { source ->
                        partial.outputStream().use { source.copyTo(it) }
                    }
                } finally {
                    disconnect()
                }
            }
            partial.renameTo(destination)
        } catch (throwable: Throwable) {
            Log.w(TAG, "Could not download font from $url", throwable)
            false
        } finally {
            if (partial.exists()) partial.delete()
        }
    }

    private fun fontDirectory(context: Context): File =
        File(context.cacheDir, "bindjs-fonts").apply { mkdirs() }
}

/**
 * The cache filename for a font URL: its SHA-256 plus the original extension, matching
 * how bindjs-apple names its own cache entries.
 *
 * Hashed rather than taken from the last path segment because two hosts routinely serve
 * different faces as `font.ttf`, and the extension is kept only so the file stays
 * recognisable on disk — nothing reads it back.
 */
internal fun cacheFileName(url: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray())
    val hash = digest.joinToString("") { "%02x".format(it) }

    // The extension has to come from the path, not the whole URL: a cache-busting
    // `?v=2` would otherwise be carried into the filename.
    val path = url.substringBefore('?').substringBefore('#')
    val extension = path.substringAfterLast('.', "")
        .takeIf { it.isNotEmpty() && it.length <= 5 && it.all(Char::isLetterOrDigit) }

    return if (extension == null) hash else "$hash.$extension"
}
