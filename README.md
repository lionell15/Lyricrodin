# Lyricrodin · Infinity-X 4.1 (Android 17)

Puente para sincronizar letras de YouTube Music con HyperLyrics Enhanced mediante Lyricon y LRCLIB. Adaptación a AOSP/Infinity-X 4.1 con notificación de letras sincronizadas opcional. Conserva el mismo paquete `org.rodin.ytmlyrics`.

**Compatibilidad:** Lyricon/HyperLyrics Enhanced puede recibir los datos del proveedor, pero la HyperIsland de Xiaomi no existe en AOSP; esta APK no la instala ni modifica SystemUI. La alternativa visible es la notificación local que muestra la línea actual.

Requisitos: Android 17; HyperLyrics Enhanced con Lyricon activado; acceso a notificaciones para leer MediaSession de YouTube Music; permiso para **enviar notificaciones** para mostrar la letra desde la propia APK. No requiere ámbito Vector adicional. Si ya usas una versión v0.3 firmada por otra sesión de Actions, puede que debas desinstalarla antes de instalar la v0.4 debido a la firma de depuración.

La compilación debug se realiza con GitHub Actions en la rama `infinity-x-4.1`; el APK de prueba se publica como artefacto de ejecución. El servicio de notificaciones sigue siendo opcional, con botón de diagnóstico y control para activar/desactivar la notificación de letras.
