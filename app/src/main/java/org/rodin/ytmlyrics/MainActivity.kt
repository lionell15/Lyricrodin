package org.rodin.ytmlyrics

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL
   gravity = Gravity.CENTER_HORIZONTAL
   setPadding(40, 70, 40, 40)
  }
  root.addView(TextView(this).apply {
   text = "YT Music → HyperIsland: Lyricrodin v0.1"
   textSize = 23f
  })
  root.addView(TextView(this).apply {
   text = "Activa Lyricon en HyperLyrics Enhanced y concede acceso a notificaciones. Reproduce YouTube Music para buscar letras sincronizadas en LRCLIB."
   textSize = 16f
   setPadding(0, 30, 0, 30)
  })
  root.addView(Button(this).apply {
   text = "Conceder acceso a notificaciones"
   setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
  })
  setContentView(root)
 }
}
