package org.rodin.ytmlyrics

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import io.github.proify.lyricon.lyric.model.RichLyricLine

/** AOSP-only presentation fallback; no HyperOS SystemUI hooks. */
internal class LyricsNotification {
    private var lastFingerprint: String? = null

    fun update(context: Context, track: Track?, lines: List<RichLyricLine>, positionMs: Long, playing: Boolean) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val enabled = context.getSharedPreferences("lyricrodin_options", Context.MODE_PRIVATE)
            .getBoolean("notifications", true)
        val permitted = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!enabled || !permitted || track == null || !playing) {
            cancel(context)
            return
        }
        val lyric = lines.lastOrNull { it.begin <= positionMs }?.text?.trim()
        val display = if (lines.isEmpty()) "Buscando letras sincronizadas…" else
            if (lyric.isNullOrBlank()) "♪" else lyric
        val fingerprint = track.id + "|" + display
        if (lastFingerprint == fingerprint) return
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel("lyricrodin_lyrics", "Letras de YouTube Music", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Letra sincronizada mientras reproduce YouTube Music"; setShowBadge(false) }
            )
        }
        val intent = PendingIntent.getActivity(
            context, 1, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(context, "lyricrodin_lyrics")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.title + " · " + track.artist)
            .setContentText(display)
            .setStyle(Notification.BigTextStyle().bigText(display))
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(intent)
            .build()
        runCatching { manager.notify(4104, notification) }
            .onSuccess { lastFingerprint = fingerprint }
            .onFailure { Log.w("Lyricrodin", "Notification not posted", it) }
    }

    fun cancel(context: Context) {
        if (lastFingerprint == null) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.cancel(4104) }
        lastFingerprint = null
    }
}
