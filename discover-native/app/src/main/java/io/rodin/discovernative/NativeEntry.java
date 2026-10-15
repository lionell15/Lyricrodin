package io.rodin.discovernative;

import android.util.Log;
import io.github.libxposed.api.XposedModule;

/**
 * Mandatory modern libxposed Java entry for Vector recognition.
 * Actual diagnostics run in native_init -> librodindiscover.so.
 * Does not hook Java code or change launcher behavior.
 */
public final class NativeEntry extends XposedModule {
    public NativeEntry() {
        Log.i("RodinDiscoverNative", "JAVA_ENTRY loaded, native diagnostic entry follows");
    }
}
