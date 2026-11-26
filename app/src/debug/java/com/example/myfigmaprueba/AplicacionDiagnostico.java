package com.example.myfigmaprueba;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** Diagnóstico local del APK debug; no envía datos a ningún servidor. */
public class AplicacionDiagnostico extends Application implements DiagnosticoInicio {
    static final String ARCHIVO = "ultimo-error-inicio.txt";
    static final String PREFS = "diagnostico";
    static final String VERSION_INFORME = "version_informe";
    private final AtomicBoolean informando = new AtomicBoolean();

    @Override protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (Build.VERSION.SDK_INT >= 28 && Application.getProcessName().endsWith(":diagnostico")) return;
        Thread.UncaughtExceptionHandler anterior = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> {
            if (!informando.compareAndSet(false, true)) {
                if (anterior != null) anterior.uncaughtException(hilo, error);
                return;
            }
            Log.e("CeroDesperdicio", "Fallo no controlado", error);
            try {
                StringWriter pila = new StringWriter(); error.printStackTrace(new PrintWriter(pila));
                String texto = "Cero Desperdicio · diagnóstico local\n"
                        + "Versión instalada: " + versionActual() + "\n"
                        + "Dispositivo: " + Build.MANUFACTURER + " " + Build.MODEL
                        + "\nAndroid: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
                        + "\nHilo: " + hilo.getName() + "\n\n" + pila;
                if (texto.length() > 16000) texto = texto.substring(0, 16000);
                try (FileOutputStream salida = new FileOutputStream(new File(getFilesDir(), ARCHIVO))) {
                    salida.write(texto.getBytes(StandardCharsets.UTF_8));
                }
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putInt(VERSION_INFORME, versionActual()).commit();
                startActivity(new Intent(this, DiagnosticoActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            } catch (Exception falloInforme) {
                Log.e("CeroDesperdicio", "No se pudo mostrar el informe", falloInforme);
            } finally {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        });
    }

    @Override public boolean mostrarInformePendiente(Activity actividad) {
        File informe = new File(getFilesDir(), ARCHIVO);
        if (!informe.exists()) return false;
        int versionInforme = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(VERSION_INFORME, -1);
        if (versionInforme != versionActual()) {
            // Una actualización debe probar su arranque, sin volver a mostrar el error de la versión anterior.
            if (!informe.renameTo(new File(getFilesDir(), "error-version-anterior.txt")))
                Log.w("CeroDesperdicio", "El informe anterior se conserva en su archivo original");
            return false;
        }
        actividad.startActivity(new Intent(actividad, DiagnosticoActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        actividad.finish(); return true;
    }

    private int versionActual() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode; }
        catch (PackageManager.NameNotFoundException e) { return -1; }
    }
}
