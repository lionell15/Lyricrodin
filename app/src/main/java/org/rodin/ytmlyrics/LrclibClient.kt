package org.rodin.ytmlyrics

import android.net.Uri
import io.github.proify.lyricon.lyric.model.RichLyricLine
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.text.Normalizer
import kotlin.math.abs

internal data class Track(val title: String, val artist: String, val album: String, val durationMs: Long) {
    val id: String get() = "ytm:" + title.lowercase() + "|" + artist.lowercase() + "|" + durationMs
}

internal data class LyricsLookup(
    val lines: List<RichLyricLine>,
    val strategy: String
)

internal class LrclibClient {
    // Cache only positive matches. An empty result or a network failure must not prevent retries.
    private val cache = object : LinkedHashMap<String, LyricsLookup>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LyricsLookup>?) = size > 64
    }

    // LRCLIB supports title-only search; artist is optional.
    fun lyrics(track: Track): LyricsLookup {
        synchronized(cache) { cache[track.id] }?.let { return it }

        val cleanTitle = cleanedTitle(track.title)
        val primaryArtist = primaryArtist(track.artist)
        val variants = linkedSetOf<Pair<String, String?>>()
        variants += track.title to track.artist
        if (primaryArtist != track.artist) variants += track.title to primaryArtist
        if (cleanTitle != track.title) {
            variants += cleanTitle to track.artist
            variants += cleanTitle to primaryArtist
        }
        variants += cleanTitle to null

        var successCount = 0
        var lastNetworkError: IOException? = null

        for ((title, artist) in variants) {
            try {
                val url = Uri.parse("https://lrclib.net/api/search").buildUpon()
                    .appendQueryParameter("track_name", title)
                    .apply { if (!artist.isNullOrBlank()) appendQueryParameter("artist_name", artist) }
                    .build().toString()
                val records = JSONArray(fetchWithRetry(url))
                successCount++
                val matched = (0 until records.length()).mapNotNull(records::optJSONObject)
                    .filter { acceptable(it, track, cleanTitle, primaryArtist) }
                    .sortedWith(
                        compareBy<JSONObject> { durationDelta(it, track) }
                            .thenByDescending { normalize(it.optString("artistName")) == normalize(primaryArtist) }
                    )

                for (record in matched.take(10)) {
                    if (record.optBoolean("instrumental", false)) continue
                    val synced = record.optString("syncedLyrics")
                        .takeUnless { it == "null" }.orEmpty()
                    if (synced.isBlank()) continue
                    val lines = LrcParser.parse(synced, track.durationMs)
                    if (lines.isNotEmpty()) {
                        val result = LyricsLookup(
                            lines,
                            "LRCLIB: " + if (artist == null) "búsqueda por título" else "búsqueda por título y artista"
                        )
                        synchronized(cache) { cache[track.id] = result }
                        return result
                    }
                }
            } catch (e: IOException) {
                lastNetworkError = e
            } catch (e: Exception) {
                // Malformed response; continue with alternative search.
                lastNetworkError = IOException(e.message ?: e.javaClass.simpleName, e)
            }
        }

        // Do not disguise a complete network outage as "lyrics not found".
        if (successCount == 0 && lastNetworkError != null) throw lastNetworkError
        return LyricsLookup(emptyList(), "LRCLIB no tiene letras sincronizadas coincidentes")
    }

    private fun fetchWithRetry(url: String): String {
        var last: IOException? = null
        repeat(2) { attempt ->
            try {
                return fetch(url)
            } catch (e: SocketTimeoutException) {
                last = e
            } catch (e: IOException) {
                last = e
            }
            if (attempt == 0) {
                try { Thread.sleep(900L) } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Interrumpido al consultar LRCLIB")
                }
            }
        }
        throw last ?: IOException("No se pudo consultar LRCLIB")
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 7_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "Lyricrodin/0.3 (Android, LRCLIB)")
        conn.setRequestProperty("Accept", "application/json")
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                if (code == 404) return "[]"
                throw IOException("LRCLIB HTTP $code")
            }
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun cleanedTitle(title: String): String {
        return title.replace(
            Regex("""(?i)\s*[\[(](official\s+(music\s+)?video|official\s+audio|lyric\s+video|lyrics?|visualizer|remaster(?:ed)?\s*\d{0,4})[\])]"""),
            ""
        ).trim().ifEmpty { title }
    }

    private fun primaryArtist(value: String): String = value.split(
        Regex("""(?i)\s+(?:y|and|feat\.?|ft\.?|featuring|with|vs\.?|x)\s+|[,;&]"""),
        limit = 2
    ).firstOrNull()?.trim().orEmpty().ifBlank { value }

    private fun normalize(input: String): String =
        Normalizer.normalize(input.lowercase().trim(), Normalizer.Form.NFKD)
            .replace(Regex("""\p{M}+"""), "")
            .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
            .trim()

    private fun durationDelta(obj: JSONObject, track: Track): Long =
        if (track.durationMs <= 0) 0L
        else abs(obj.optLong("duration", -1L) * 1000L - track.durationMs)

    private fun acceptable(obj: JSONObject, track: Track, cleanTitle: String, primaryArtist: String): Boolean {
        val foundTitle = normalize(cleanedTitle(obj.optString("trackName")))
        val expectedTitle = normalize(cleanTitle)
        if (foundTitle.isBlank() || foundTitle != expectedTitle) return false

        val foundArtist = normalize(obj.optString("artistName"))
        val expectedFull = normalize(track.artist)
        val expectedMain = normalize(primaryArtist)
        val foundMain = normalize(primaryArtist(obj.optString("artistName")))
        if (foundArtist.isBlank() || expectedMain.isBlank()) return false

        // Require actual primary artist agreement. Do not accept arbitrary songs with same title.
        val artistMatches = foundArtist == expectedFull ||
            foundArtist == expectedMain ||
            foundMain == expectedMain ||
            foundArtist.startsWith("$expectedMain ") ||
            expectedFull.startsWith("$foundMain ")
        if (!artistMatches) return false

        // Avoid mismatched live/remix versions where timings differ materially.
        return track.durationMs <= 0 || durationDelta(obj, track) <= 15_000L
    }
}
