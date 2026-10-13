package org.rodin.ytmlyrics

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.proify.lyricon.lyric.model.RichLyricLine
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.provider.LyriconFactory
import io.github.proify.lyricon.provider.LyriconProvider
import java.util.concurrent.Executors

class YtMusicListener:NotificationListenerService() {
 private val main=Handler(Looper.getMainLooper())
 private val worker=Executors.newSingleThreadExecutor()
 private val client=LrclibClient()
 private var manager:MediaSessionManager?=null
 private var current:MediaController?=null
 private var provider:LyriconProvider?=null
 private var track:Track?=null
 private var lastPlaying:Boolean?=null
 private var serial=0L
 private var running=false
 private var lastCentralConnected:Boolean?=null

 private val callback=object:MediaController.Callback() {
  override fun onMetadataChanged(metadata:MediaMetadata?)=update()
  override fun onPlaybackStateChanged(state:PlaybackState?)=update()
  override fun onSessionDestroyed()=refresh()
 }
 private val sessions=MediaSessionManager.OnActiveSessionsChangedListener {refresh()}
 private val tick=object:Runnable {
  override fun run() {
   if(!running)return
   val active=provider?.player?.isActive==true
   if(lastCentralConnected!=active) {
    lastCentralConnected=active
    DiagnosticStore.put(this@YtMusicListener,"central",if(active)"Conectado con Lyricon" else "Sin conexión con Lyricon")
   }
   val p=current?.playbackState
   val playing=p?.state==PlaybackState.STATE_PLAYING
   if(lastPlaying!=playing) {
    provider?.player?.setPlaybackState(playing)
    lastPlaying=playing
   }
   provider?.player?.setPosition(position(p))
   main.postDelayed(this,1000)
  }
 }
 override fun onListenerConnected() {
  super.onListenerConnected()
  main.post {
   if(running)return@post
   running=true
   DiagnosticStore.put(this@YtMusicListener,"service","Servicio de notificaciones activo")
   manager=getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
   provider=LyriconFactory.createProvider(
    context=applicationContext,
    providerPackageName=packageName,
    playerPackageName=YTM
   ).also {
    it.autoSync=true
    val ok=it.register()
    DiagnosticStore.put(this@YtMusicListener,"register","Solicitud de registro Lyricon: "+ok)
   }
   runCatching {
    manager?.addOnActiveSessionsChangedListener(
     sessions,ComponentName(this,YtMusicListener::class.java),main)
   }.onFailure {
    Log.e(TAG,"Unable to observe MediaSession",it)
    DiagnosticStore.put(this@YtMusicListener,"error","MediaSession: "+it.javaClass.simpleName+": "+it.message)
   }
   refresh()
   main.removeCallbacks(tick)
   main.post(tick)
  }
 }
 override fun onNotificationPosted(sbn:StatusBarNotification?) {
  if(sbn?.packageName==YTM)main.post {refresh()}
 }
 override fun onNotificationRemoved(sbn:StatusBarNotification?) {
  if(sbn?.packageName==YTM)main.post {refresh()}
 }
 private fun refresh() {
  if(!running)return
  val all=runCatching {
   manager?.getActiveSessions(ComponentName(this,YtMusicListener::class.java)).orEmpty()
  }.getOrElse {emptyList()}
  val ytmSessions=all.count {it.packageName==YTM}
  DiagnosticStore.put(this,"session",if(ytmSessions>0)"Sesión YouTube Music detectada" else "No hay sesión de YouTube Music; inicia una canción")
  val next=all.filter {it.packageName==YTM}.maxByOrNull {
   when(it.playbackState?.state) {
    PlaybackState.STATE_PLAYING->3
    PlaybackState.STATE_BUFFERING->2
    PlaybackState.STATE_PAUSED->1
    else->0
   }
  }
  if(current?.sessionToken!=next?.sessionToken) {
   runCatching {current?.unregisterCallback(callback)}
   current=next
   lastPlaying=null
   next?.registerCallback(callback,main)
  }
  update()
 }
 private fun update() {
  if(!running)return
  val data=current?.metadata
  val title=data?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
  val artist=data?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
  val album=data?.getString(MediaMetadata.METADATA_KEY_ALBUM)?.trim().orEmpty()
  val duration=data?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0)?:0
  if(title.isBlank() || artist.isBlank()) {
   if(track!=null) DiagnosticStore.put(this,"track","Sin metadatos de canción")
   if(track!=null) {
    serial++
    track=null
    provider?.player?.setSong(null)
    provider?.player?.setPlaybackState(false)
   }
   return
  }
  val next=Track(title,artist,album,duration)
  if(next.id!=track?.id) {
   serial++
   val seq=serial
   track=next
   DiagnosticStore.put(this,"track",title+" — "+artist)
   DiagnosticStore.put(this,"lyrics","Buscando letras sincronizadas en LRCLIB…")
   lastPlaying=null
   publish(next,emptyList())
   worker.execute {
    val lyrics=runCatching {client.lyrics(next)}.getOrElse {
     Log.e(TAG,"Lyrics request error",it)
     DiagnosticStore.put(this@YtMusicListener,"error","LRCLIB: "+it.javaClass.simpleName+": "+it.message)
     emptyList()
    }
    main.post {
     if(running && seq==serial && track?.id==next.id) {
      publish(next,lyrics)
      DiagnosticStore.put(this@YtMusicListener,"lyrics",if(lyrics.isEmpty())"No hay letras sincronizadas para esta canción" else "Letras recibidas: "+lyrics.size+" líneas")
      Log.i(TAG,"Lyrics lines: "+lyrics.size)
     }
    }
   }
  }
  val playing=current?.playbackState?.state==PlaybackState.STATE_PLAYING
  if(lastPlaying!=playing) {
   provider?.player?.setPlaybackState(playing)
   lastPlaying=playing
  }
  provider?.player?.setPosition(position(current?.playbackState))
 }
 private fun publish(t:Track,lines:List<RichLyricLine>) {
  val sent=provider?.player?.setSong(Song(
   id=t.id,name=t.title,artist=t.artist,duration=t.durationMs,lyrics=lines))
  DiagnosticStore.put(this,"delivery","setSong(): "+sent+" (líneas: "+lines.size+")")
  provider?.player?.setPosition(position(current?.playbackState))
  provider?.player?.setPlaybackState(
   current?.playbackState?.state==PlaybackState.STATE_PLAYING)
 }
 private fun position(s:PlaybackState?):Long {
  if(s==null)return 0
  val raw=s.position.coerceAtLeast(0)
  if(s.state!=PlaybackState.STATE_PLAYING)return raw
  val elapsed=(SystemClock.elapsedRealtime()-s.lastPositionUpdateTime).coerceAtLeast(0)
  return (raw+elapsed*s.playbackSpeed).toLong().coerceAtLeast(0)
 }
 override fun onListenerDisconnected() {
  super.onListenerDisconnected()
  main.post {stop()}
 }
 private fun stop() {
  running=false
  DiagnosticStore.put(this,"service","Servicio detenido / sin acceso")
  serial++
  main.removeCallbacks(tick)
  runCatching {current?.unregisterCallback(callback)}
  current=null
  runCatching {manager?.removeOnActiveSessionsChangedListener(sessions)}
  manager=null
  runCatching {provider?.player?.setSong(null);provider?.unregister();provider?.destroy()}
  provider=null
  track=null
 }
 override fun onDestroy() {
  stop()
  worker.shutdownNow()
  super.onDestroy()
 }
 companion object {
  private const val TAG="Lyricrodin"
  private const val YTM="com.google.android.apps.youtube.music"
 }
}
