# Lyricrodin v0.5 · YouTube Music + Morphe

Puente para enviar letras sincronizadas desde **YouTube Music oficial y Morphe** a **Lyricon / HyperLyrics Enhanced**. Partimos de la versión funcional v0.4 de Infinity-X, manteniendo su servicio de notificaciones y la búsqueda de letras en LRCLIB.

## Reproductores admitidos

- `com.google.android.apps.youtube.music` — YouTube Music oficial (o Morphe parcheado conservando el paquete).
- `app.morphe.android.apps.youtube.music` — paquete habitual de Morphe.
- `com.morphe.android.apps.youtube.music` — variante Morphe.
- `com.google.android.apps.youtube.music.morphe` — variante con sufijo Morphe.

La app observa las MediaSession activas. Cuando cambia de reproductor, desregistra el proveedor anterior y registra uno nuevo para el paquete detectado: Lyricon necesita que `playerPackageName` coincida con el reproductor real. Conserva el id `org.rodin.ytmlyrics`. No modifica, repaqueta ni requiere módulos Xposed para YouTube Music/Morphe.

## Instalación y configuración

1. Instala el APK compilado por GitHub Actions en la rama `morphe-ytmusic` (archivo de artefactos `Lyricrodin-Morphe-v0.5-debug-apk`).
2. Abre Lyricrodin y concede **acceso a notificaciones** (es necesario para leer MediaSession). Concede enviar notificaciones si vas a usar su visualización de letras local.
3. Reproduce música en Morphe. En el diagnóstico de Lyricrodin debe aparecer **Sesión YouTube Music/Morphe detectada**, su **paquete**, registro Lyricon y letra recuperada.
4. En HyperLyrics Enhanced, comprueba el módulo / Central y selecciona **Lyricon** cuando el modo correspondiente sea compatible. No instales otro proveedor simultáneo para el mismo reproductor.

**XPerience/Infinity-X (AOSP, Android 17):** Estas ROM no incluyen la HyperIsland de Xiaomi. HyperLyrics Enhanced no puede añadir el soporte HyperIsland a un SystemUI AOSP por sí solo. Lyricrodin dispone de una notificación propia con la línea sincronizada como alternativa, activable en su pantalla principal.

**Compatibilidad:** el paquete Morphe puede personalizarse al parchear. Si el diagnóstico no detecta la sesión, verifica el identificador instalado con `su -c "pm list packages | grep -i -e morphe -e youtube.music"`. La compilación debug usa la firma de Actions y puede exigir desinstalar una v0.4 debug con firma distinta antes de instalar. Las letras dependen de LRCLIB, la metadata publicada por el reproductor y la conexión con Lyricon. El envío de datos a HyperLyrics Enhanced aún debe comprobarse sobre el dispositivo.
