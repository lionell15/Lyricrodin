package org.rodin.ytmlyrics

import io.github.proify.lyricon.lyric.model.RichLyricLine

internal object LrcParser {
 private val stamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.,](\d{1,3}))?]""")
 fun parse(source: String, durationMs: Long): List<RichLyricLine> {
  val parts = mutableListOf<Pair<Long, String>>()
  for (line in source.lineSequence()) {
   val matches = stamp.findAll(line).toList()
   val lyric = stamp.replace(line, "").trim()
   if (lyric.isBlank()) continue
   for (m in matches) {
    val mins = m.groupValues[1].toLong()
    val secs = m.groupValues[2].toLong()
    val frac = m.groupValues[3]
    val ms = if (frac.isEmpty()) 0L else frac.padEnd(3,'0').take(3).toLong()
    parts += (mins*60000L + secs*1000L + ms) to lyric
   }
  }
  val ordered = parts.distinct().sortedBy { it.first }
  return ordered.mapIndexed { i, (begin, lyric) ->
   val end = (ordered.getOrNull(i+1)?.first ?: durationMs.coerceAtLeast(begin+5000L)).coerceAtLeast(begin+100L)
   RichLyricLine(begin=begin, end=end, text=lyric)
  }
 }
}
