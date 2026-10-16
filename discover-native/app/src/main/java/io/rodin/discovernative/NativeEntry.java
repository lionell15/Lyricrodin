package io.rodin.discovernative;

import android.util.Log;
import io.github.libxposed.api.XposedModule;

/**
 * Vector registers native_init.list, but the Java entry must load the .so.
 * Scoped strictly to com.miui.home by scope.list.
 */
public final class NativeEntry extends XposedModule {
    public NativeEntry() {
        Log.i("RodinDiscoverNative", "JAVA_ENTRY: constructor entered, loading native library");
        try {
            System.loadLibrary("rodindiscover");
            Log.i("RodinDiscoverNative", "JAVA_ENTRY: System.loadLibrary OK");
        } catch (Throwable error) {
            Log.e("RodinDiscoverNative", "JAVA_ENTRY: load failed", error);
        }
    }
}
