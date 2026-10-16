# Rodin Discover Native v0.7.2 — Vector API 102

**Estado:** experimental, solo diagnóstico; no habilita todavía Google Discover en pantalla -1.

Se corrige v0.7.1: Vector registra `native_init.list` pero no ejecuta automáticamente `System.loadLibrary()`. La clase `NativeEntry` carga explícitamente `librodindiscover.so`.

- ROM objetivo: HOS4 oz7wm, rodin, Android 17, launcher 8.01.02.6325.
- Ámbito único: `com.miui.home`.
- Firma CRC32 de `.text`: `f262e085` (binario original).
- No cambia la región ni activa el forzado de una llamada.
- Mantén `persist.rodin.discover.enable` sin activar.

**Pasos**: desinstalar APK experimental previo si falla la instalación (cada compilación GitHub Actions utiliza una firma debug distinta), instalar la nueva, habilitar en Vector solo para `com.miui.home`, reiniciar y deslizar hasta la pantalla -1.

Capturar el estado:
```sh
su -c "logcat -b all -d -v brief | grep -Ei 'RodinDiscoverNative|librodindiscover|VectorModule|native_init' > /sdcard/Download/rodin_discover_native_v072.txt"
```

Mensajes esperados: `JAVA_ENTRY`, `native_init`, `READY` o `SKIP`.
Si está vacío, revisar ámbito, estado de Vector y si el launcher se reinició realmente.
