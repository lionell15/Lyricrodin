package org.rodin.ytmlyrics

import android.net.Uri
import io.github.proify.lyricon.lyric.model.RichLyricLine
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import kotlin.math.abs

internal data class Track(val title:String, val artist:String, val album:String, val durationMs:Long) {
 val id:String get() = "ytm:" + title.lowercase() + "|" + artist.lowercase() + "|" + durationMs
}
internal class LrclibClient {
 private val cache = mutableMapOf<String,List<RichLyricLine>>()
 fun lyrics(track:Track):List<RichLyricLine> {
  cache[track.id]?.let { return it }
  val url = Uri.parse("https://lrclib.net/api/search").buildUpon()
    .appendQueryParameter("track_name", track.title)
    .appendQueryParameter("artist_name", track.artist).build().toString()
  val arr = JSONArray(fetch(url))
  val matches = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
   .filter { match(it,track) }.sortedBy { difference(it,track) }
  val found = matches.firstNotNullOfOrNull { obj ->
   val data = obj.optString("syncedLyrics").takeUnless { it == "null" }.orEmpty()
   if (data.isBlank() || obj.optBoolean("instrumental")) null
   else LrcParser.parse(data,track.durationMs).takeIf { it.isNotEmpty() }
  } ?: emptyList()
  cache[track.id] = found
  return found
 }
 private fun difference(j:JSONObject,t:Track):Long =
  if (t.durationMs <= 0) 0 else abs(j.optLong("duration",-1L)*1000 - t.durationMs)
 private fun norm(s:String):String = Normalizer.normalize(s.lowercase().trim(),Normalizer.Form.NFKD)
   .replace(Regex("""\p{M}+"""),"").replace(Regex("""[^\p{L}\p{N}]+""")," ").trim()
 private fun match(j:JSONObject,t:Track):Boolean {
  val title=norm(j.optString("trackName"))
  val artist=norm(j.optString("artistName"))
  val wantedArtist=norm(t.artist)
  return title==norm(t.title) && artist.isNotBlank() &&
   (artist.contains(wantedArtist) || wantedArtist.contains(artist)) &&
   (t.durationMs<=0 || difference(j,t)<8000)
 }
 private fun fetch(url:String):String {
  val conn=URL(url).openConnection() as HttpURLConnection
  conn.connectTimeout=6000
  conn.readTimeout=6000
  conn.setRequestProperty("User-Agent","Lyricrodin/0.1 (Android, noncommercial)")
  try {
   check(conn.responseCode in 200..299) { "LRCLIB returned " + conn.responseCode }
   return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
  } finally {conn.disconnect()}
 }
}
