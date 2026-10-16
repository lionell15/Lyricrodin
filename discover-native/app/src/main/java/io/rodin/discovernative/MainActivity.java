package io.rodin.discovernative;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.widget.TextView;
public class MainActivity extends Activity {
  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TextView status = new TextView(this);
    status.setTextColor(Color.WHITE);
    status.setBackgroundColor(Color.rgb(23, 26, 33));
    status.setTextSize(17f);
    status.setPadding(40, 52, 40, 40);
    status.setText("Rodin Discover Native v0.7.2\n\n"
        + "Estado: modo diagnóstico (no sustituye todavía la Bóveda).\n\n"
        + "1. Activa este módulo en Vector con ámbito com.miui.home.\n"
        + "2. Reinicia el dispositivo.\n"
        + "3. Captura logs en Termux:\n\n"
        + "su -c \"logcat -b all -d -v brief | grep -i RodinDiscoverNative\"\n\n"
        + "Comprueba JAVA_ENTRY y native_init para confirmar la inyección.");
    setContentView(status);
  }
}
