package org.rodin.ytmlyrics

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager

internal object DiagnosticStore {
 private const val PREFS="lyricrodin_diagnostics"
 fun put(ctx:Context,key:String,value:String) {
  val prefs=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
  if(prefs.getString(key,null)==value)return
  prefs.edit().putString(key,value).apply()
  Log.i("Lyricrodin",key+": "+value)
 }
 fun render(ctx:Context):String {
  val enabled=Settings.Secure.getString(ctx.contentResolver,"enabled_notification_listeners")
    ?.contains(ctx.packageName)==true
  val prefs=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
  return buildString {
   append("Acceso a notificaciones: ")
   append(if(enabled)"CONCEDIDO" else "NO CONCEDIDO")
   append("\n\n")
   val options=ctx.getSharedPreferences("lyricrodin_options",Context.MODE_PRIVATE)
   val notificationFlag=options.getBoolean("notifications",true)
   val notificationGranted=Build.VERSION.SDK_INT<33 || ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
   append("Letras en notificación AOSP: ")
   append(if(!notificationFlag)"DESACTIVADAS" else if(notificationGranted)"ACTIVADAS" else "FALTA PERMISO")
   append("\n\n")
   val labels=listOf(
    "service" to "Servicio",
    "session" to "YouTube Music",
    "track" to "Canción",
    "register" to "Registro Lyricon",
    "central" to "Conexión",
    "lyrics" to "Letras LRCLIB",
    "delivery" to "Envío a Lyricon",
    "error" to "Último error"
   )
   for((key,label) in labels) {
    append(label).append(": ").append(prefs.getString(key,"Sin datos")).append("\n\n")
   }
  }
 }
}
