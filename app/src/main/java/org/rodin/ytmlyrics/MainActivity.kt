package org.rodin.ytmlyrics

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Switch
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity:Activity() {
 private val handler=Handler(Looper.getMainLooper())
 private lateinit var status:TextView
 private val refresh=object:Runnable {
  override fun run() {
   status.text=DiagnosticStore.render(this@MainActivity)
   handler.postDelayed(this,2000L)
  }
 }
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply {
   orientation=LinearLayout.VERTICAL
   setPadding(35,50,35,35)
  }
  root.addView(TextView(this).apply {
   text="Lyricrodin v0.5 · YouTube Music + Morphe"
   textSize=23f
  })
  root.addView(TextView(this).apply {
   text="Proveedor para YouTube Music oficial y Morphe → Lyricon / HyperLyrics Enhanced. Detecta el reproductor activo y recupera letras sincronizadas de LRCLIB. En ROMs AOSP como XPerience o Infinity-X no existe la HyperIsland de Xiaomi; activa la notificación de letras si quieres verlas en Android."
   textSize=15f
   setPadding(0,25,0,25)
  })
  root.addView(Button(this).apply {
   text="Permiso de acceso a notificaciones"
   setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
  })
  root.addView(Switch(this).apply {
   text="Mostrar letra sincronizada en notificación"
   textSize=16f
   isChecked=getSharedPreferences("lyricrodin_options",MODE_PRIVATE).getBoolean("notifications",true)
   setOnCheckedChangeListener { _, enabled ->
    getSharedPreferences("lyricrodin_options",MODE_PRIVATE).edit().putBoolean("notifications",enabled).apply()
    if(enabled && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
     requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),100)
    }
   }
  })
  root.addView(Button(this).apply {
   text="Copiar diagnóstico"
   setOnClickListener {
    val text=DiagnosticStore.render(this@MainActivity)
    val clipboard=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Lyricrodin diagnóstico",text))
    android.widget.Toast.makeText(this@MainActivity,"Copiado",android.widget.Toast.LENGTH_SHORT).show()
   }
  })
  status=TextView(this).apply {
   textSize=16f
   setPadding(0,25,0,40)
  }
  root.addView(status)
  setContentView(ScrollView(this).apply {addView(root)})
  if (Build.VERSION.SDK_INT>=33 && getSharedPreferences("lyricrodin_options",MODE_PRIVATE).getBoolean("notifications",true)
   && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
   requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),100)
  }
 }
 override fun onResume() {
  super.onResume()
  handler.removeCallbacks(refresh)
  handler.post(refresh)
 }
 override fun onPause() {
  handler.removeCallbacks(refresh)
  super.onPause()
 }
}
