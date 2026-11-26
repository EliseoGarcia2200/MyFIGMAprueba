package com.example.myfigmaprueba;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.recyclerview.widget.RecyclerView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Reproduce una base marcada como v4 a la que le falta publicaciones.eliminado. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36)
public class MigracionSQLiteTest {
    private long cliente, panaderia, producto, pedido;
    private String token;

    private void prepararVersionCuatro(Context contexto, boolean faltaEliminado) {
        try (DBHelper db = new DBHelper(contexto)) {
            panaderia = db.registrarUsuario("Panadería existente", "pan-migracion@test.invalid", "clave123", Usuario.ROL_PANADERIA);
            cliente = db.registrarUsuario("Cliente existente", "cliente-migracion@test.invalid", "clave123", Usuario.ROL_CLIENTE);
            db.guardarNegocio(panaderia, "Dirección guardada", "Lunes a sábado 16–20 h", "", "", null, null);
            producto = db.insertarPublicacion(new Publicacion(-1, panaderia, "Pan existente", 15.50, "Del día", 3, false, null));
            pedido = db.reservar(cliente, producto, 1, "solicitud-anterior");
            token = db.obtenerPedido(pedido, cliente).token;
            new SesionManager(contexto).iniciar(db.obtenerUsuario(cliente));
            SQLiteDatabase sqlite = db.getWritableDatabase();
            if (faltaEliminado) {
                sqlite.execSQL("ALTER TABLE publicaciones RENAME TO publicaciones_fixture");
                sqlite.execSQL("CREATE TABLE publicaciones (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "panaderia_id INTEGER NOT NULL, nombre TEXT NOT NULL, precio REAL NOT NULL, "
                        + "detalles TEXT NOT NULL DEFAULT '', cantidad INTEGER NOT NULL, "
                        + "caja_sorpresa INTEGER NOT NULL DEFAULT 0, imagen TEXT, creado INTEGER NOT NULL)");
                sqlite.execSQL("INSERT INTO publicaciones (id, panaderia_id, nombre, precio, detalles, cantidad, caja_sorpresa, imagen, creado) "
                        + "SELECT id, panaderia_id, nombre, precio, detalles, cantidad, caja_sorpresa, imagen, creado FROM publicaciones_fixture");
                sqlite.execSQL("DROP TABLE publicaciones_fixture");
            }
            sqlite.setVersion(4);
        }
    }

    @Test public void reparaVersionCuatroSinEliminadoYConservaUsuariosPublicacionesYPedidos() {
        Context contexto = RuntimeEnvironment.getApplication();
        prepararVersionCuatro(contexto, true);
        try (DBHelper db = new DBHelper(contexto)) {
            // Esta consulta reproducía exactamente el cierre del Motorola en onResume.
            assertEquals(1, db.listarLocales("").size());
            assertEquals(5, db.getReadableDatabase().getVersion());
            assertEquals("Pan existente", db.obtenerProducto(producto).nombre);
            assertEquals(2, db.obtenerProducto(producto).cantidad);
            assertEquals("Dirección guardada", db.obtenerNegocio(panaderia).direccion);
            assertEquals(cliente, db.iniciarSesion("cliente-migracion@test.invalid", "clave123").id);
            assertEquals(cliente, new SesionManager(contexto).getId());
            Pedido conservado = db.obtenerPedido(pedido, cliente);
            assertEquals(token, conservado.token); assertEquals(1550, conservado.totalCentavos());
            assertEquals(pedido, db.reservar(cliente, producto, 1, "solicitud-anterior"));
            assertEquals(2, db.obtenerProducto(producto).cantidad);
        }
        try (DBHelper reabierta = new DBHelper(contexto)) {
            assertEquals(1, reabierta.listarLocales("").size());
            assertEquals(1, reabierta.listarPedidos(cliente, "").size());
        }
    }

    @Test public void buscadorReanudaConSesionYBaseVersionCuatroIncompleta() {
        Context contexto = RuntimeEnvironment.getApplication();
        prepararVersionCuatro(contexto, true);
        try (ActivityController<BuscarLocalesActivity> pantalla = Robolectric.buildActivity(BuscarLocalesActivity.class)) {
            BuscarLocalesActivity actividad = pantalla.setup().get();
            assertFalse(actividad.isFinishing());
            RecyclerView locales = actividad.findViewById(R.id.rvLocales);
            assertEquals(1, locales.getAdapter().getItemCount());
            assertEquals(cliente, new SesionManager(contexto).getId());
        }
    }

    @Test public void versionCuatroCompletaConservaPublicacionesRetiradasYPedidos() {
        Context contexto = RuntimeEnvironment.getApplication();
        prepararVersionCuatro(contexto, false);
        try (DBHelper db = new DBHelper(contexto)) {
            assertEquals(5, db.getReadableDatabase().getVersion());
            assertTrue(db.eliminarPublicacion(producto, panaderia));
        }
        // Simula otra base v4 válida con publicaciones retiradas e historial existente.
        try (SQLiteDatabase anterior = contexto.openOrCreateDatabase("cero_desperdicio.db", Context.MODE_PRIVATE, null)) {
            anterior.setVersion(4);
        }
        try (DBHelper migrada = new DBHelper(contexto)) {
            assertEquals(5, migrada.getReadableDatabase().getVersion());
            assertTrue(migrada.listarLocales("").isEmpty());
            assertNull(migrada.obtenerProducto(producto));
            assertEquals(token, migrada.obtenerPedido(pedido, cliente).token);
            assertEquals(1, migrada.listarPedidos(cliente, "").size());
        }
    }

    @Test public void migraVersionesUnoDosYTresSinBorrarDatos() {
        Context contexto = RuntimeEnvironment.getApplication();
        for (int version = 1; version <= 3; version++) {
            String nombre = "migracion-v" + version + ".db";
            try {
                try (SQLiteDatabase anterior = contexto.openOrCreateDatabase(nombre, Context.MODE_PRIVATE, null)) {
                    anterior.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, correo TEXT NOT NULL UNIQUE, "
                            + "password_hash TEXT NOT NULL, salt TEXT NOT NULL, rol TEXT NOT NULL, creado INTEGER NOT NULL)");
                    anterior.execSQL("INSERT INTO usuarios VALUES (7, 'Negocio anterior', 'anterior@test.invalid', 'hash', 'sal', 'panaderia', 1)");
                    if (version >= 2) {
                        anterior.execSQL("CREATE TABLE publicaciones (id INTEGER PRIMARY KEY, panaderia_id INTEGER, nombre TEXT, "
                                + "precio REAL, detalles TEXT, cantidad INTEGER, caja_sorpresa INTEGER, creado INTEGER"
                                + (version >= 3 ? ", imagen TEXT" : "") + ")");
                        anterior.execSQL("INSERT INTO publicaciones (id, panaderia_id, nombre, precio, detalles, cantidad, caja_sorpresa, creado) "
                                + "VALUES (9, 7, 'Pan anterior', 15.5, '', 4, 0, 1)");
                    }
                    anterior.setVersion(version);
                }
                try (DBHelper migrada = new DBHelper(contexto, nombre)) {
                    assertEquals(5, migrada.getReadableDatabase().getVersion());
                    assertEquals("Negocio anterior", migrada.obtenerUsuario(7).nombre);
                    assertTrue(migrada.listarPedidos(7, "").isEmpty());
                    assertEquals(version >= 2 ? 1 : 0, migrada.listarLocales("").size());
                    if (version >= 2) assertEquals(4, migrada.obtenerProducto(9).cantidad);
                }
            } finally { contexto.deleteDatabase(nombre); }
        }
    }
}
