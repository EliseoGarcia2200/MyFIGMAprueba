package com.example.myfigmaprueba;

import android.content.Context;
import android.content.Intent;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Regresión del arranque en la versión de Android reportada por el usuario. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class ArranqueAndroid16Test {
    @Test public void instalacionNuevaMuestraLogin() {
        Context app = RuntimeEnvironment.getApplication();
        new SesionManager(app).cerrar();
        try (ActivityController<LoginActivity> pantalla = Robolectric.buildActivity(LoginActivity.class)) {
            LoginActivity login = pantalla.setup().get();
            assertNotNull(login.findViewById(R.id.btnIniciarSesion));
            assertNotNull(login.findViewById(R.id.etCorreo));
        }
    }

    @Test public void registroSePuedeAbrir() {
        try (ActivityController<RegistroActivity> pantalla = Robolectric.buildActivity(RegistroActivity.class)) {
            assertNotNull(pantalla.setup().get().findViewById(R.id.btnRegistrar));
        }
    }

    @Test public void sesionDePanaderiaAbreSuInicio() {
        Context app = RuntimeEnvironment.getApplication();
        try (DBHelper db = new DBHelper(app)) {
            long id = db.registrarUsuario("Panadería", "inicio-pan@test.invalid", "clave123", Usuario.ROL_PANADERIA);
            new SesionManager(app).iniciar(db.obtenerUsuario(id));
        }
        try (ActivityController<LoginActivity> login = Robolectric.buildActivity(LoginActivity.class)) {
            LoginActivity actividad = login.create().get();
            Intent destino = Shadows.shadowOf(actividad).getNextStartedActivity();
            assertEquals(InicioPanaderiaActivity.class.getName(), destino.getComponent().getClassName());
        }
        try (ActivityController<InicioPanaderiaActivity> inicio = Robolectric.buildActivity(InicioPanaderiaActivity.class)) {
            assertNotNull(inicio.setup().get().findViewById(R.id.tvSaludo));
        }
    }

    @Test public void sesionDeClienteAbreBuscador() {
        Context app = RuntimeEnvironment.getApplication();
        try (DBHelper db = new DBHelper(app)) {
            long id = db.registrarUsuario("Cliente", "inicio-cliente@test.invalid", "clave123", Usuario.ROL_CLIENTE);
            new SesionManager(app).iniciar(db.obtenerUsuario(id));
        }
        try (ActivityController<BuscarLocalesActivity> inicio = Robolectric.buildActivity(BuscarLocalesActivity.class)) {
            assertNotNull(inicio.setup().get().findViewById(R.id.rvLocales));
        }
    }

    @Test public void informePendienteAbreDiagnosticoSinBorrarDatos() throws Exception {
        Context app = RuntimeEnvironment.getApplication();
        try (DBHelper db = new DBHelper(app)) {
            long id = db.registrarUsuario("Cliente", "diagnostico@test.invalid", "clave123", Usuario.ROL_CLIENTE);
            new SesionManager(app).iniciar(db.obtenerUsuario(id));
            File informe = new File(app.getFilesDir(), AplicacionDiagnostico.ARCHIVO);
            try (FileOutputStream salida = new FileOutputStream(informe)) {
                salida.write("Error de prueba".getBytes(StandardCharsets.UTF_8));
            }
            app.getSharedPreferences(AplicacionDiagnostico.PREFS, Context.MODE_PRIVATE).edit()
                    .putInt(AplicacionDiagnostico.VERSION_INFORME,
                            app.getPackageManager().getPackageInfo(app.getPackageName(), 0).versionCode).commit();
            try (ActivityController<LoginActivity> login = Robolectric.buildActivity(LoginActivity.class)) {
                Intent siguiente = Shadows.shadowOf(login.create().get()).getNextStartedActivity();
                assertEquals(DiagnosticoActivity.class.getName(), siguiente.getComponent().getClassName());
                assertNotNull(db.obtenerUsuario(id)); assertEquals(id, new SesionManager(app).getId());
            } finally { informe.delete(); }
        }
    }

    @Test public void pantallaDeDiagnosticoSeAbreEnAndroid16() throws Exception {
        Context app = RuntimeEnvironment.getApplication();
        File informe = new File(app.getFilesDir(), AplicacionDiagnostico.ARCHIVO);
        try (FileOutputStream salida = new FileOutputStream(informe)) {
            salida.write("Error de prueba en Android 16".getBytes(StandardCharsets.UTF_8));
        }
        try (ActivityController<DiagnosticoActivity> pantalla = Robolectric.buildActivity(DiagnosticoActivity.class)) {
            assertFalse(pantalla.setup().get().isFinishing());
        } finally { informe.delete(); }
    }

    @Test public void informeDeVersionAnteriorNoBloqueaActualizacion() throws Exception {
        Context app = RuntimeEnvironment.getApplication();
        new SesionManager(app).cerrar();
        File informe = new File(app.getFilesDir(), AplicacionDiagnostico.ARCHIVO);
        File archivado = new File(app.getFilesDir(), "error-version-anterior.txt");
        try (FileOutputStream salida = new FileOutputStream(informe)) {
            salida.write("Error de la versión anterior".getBytes(StandardCharsets.UTF_8));
        }
        // Los informes del primer APK diagnóstico no guardaban el número de versión.
        app.getSharedPreferences(AplicacionDiagnostico.PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        try (ActivityController<LoginActivity> login = Robolectric.buildActivity(LoginActivity.class)) {
            assertNotNull(login.setup().get().findViewById(R.id.btnIniciarSesion));
            assertFalse(informe.exists()); assertTrue(archivado.exists());
        } finally { informe.delete(); archivado.delete(); }
    }
}
