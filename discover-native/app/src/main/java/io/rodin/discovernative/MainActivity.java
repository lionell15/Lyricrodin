package io.rodin.discovernative;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  TextView t = new TextView(this);
  t.setPadding(36,36,36,36);
  t.setText("Rodin Discover v0.7 — experimental.\n\n"
     + "Solo diagnostica el hook nativo. NO sustituye la Boveda.\n"
     + "En Vector activa unicamente com.miui.home.\n"
     + "Consulta el logcat con la etiqueta RodinDiscoverNative.");
  setContentView(t);
 }
}
