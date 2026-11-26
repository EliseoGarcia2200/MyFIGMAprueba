package com.example.myfigmaprueba;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.junit.Assert.*;

/** Bases aisladas por prueba: nunca borra ni modifica las cuentas de la aplicación. */
@RunWith(AndroidJUnit4.class)
public class ReservasTest {
    private Context context;
    private String nombre;
    private DBHelper db;
    private long vendedor, cliente, otro, producto;

    @Before public void preparar() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        nombre = "prueba_reservas_" + UUID.randomUUID() + ".db";
        db = new DBHelper(context, nombre);
        vendedor = db.registrarUsuario("Panadería", "pan@test.invalid", "clave123", Usuario.ROL_PANADERIA);
        cliente = db.registrarUsuario("Cliente", "cliente@test.invalid", "clave123", Usuario.ROL_CLIENTE);
        otro = db.registrarUsuario("Otro", "otro@test.invalid", "clave123", Usuario.ROL_CLIENTE);
        assertTrue(db.guardarNegocio(vendedor, "Calle 1", "Lunes a sábado 16–20 h", "", "", 19.04, -98.2));
        producto = db.insertarPublicacion(new Publicacion(-1, vendedor, "Pan", 12.35, "Del día", 5, false, null));
        assertTrue(producto > 0);
    }
    @After public void limpiar() { db.close(); context.deleteDatabase(nombre); }
    private long reservar(int cantidad) { return db.reservar(cliente, producto, cantidad, UUID.randomUUID().toString()); }
    private int stock() { return db.obtenerProducto(producto).cantidad; }

    @Test public void reservaDescuentaYConservaPrecioYDireccion() {
        long id = reservar(2);
        assertEquals(3, stock());
        assertEquals(1, db.listarPedidos(cliente, "").size());
        assertEquals(1, db.listarPedidos(vendedor, "").size());
        assertTrue(db.listarPedidos(otro, "").isEmpty());
        assertTrue(db.actualizarPublicacion(new Publicacion(producto, vendedor, "Nombre nuevo", 40, "Cambio", 3, false, null)));
        db.guardarNegocio(vendedor, "Nueva dirección", "Otro horario", "", "", null, null);
        Pedido p = db.obtenerPedido(id, cliente);
        assertEquals("Pan", p.producto); assertEquals("Calle 1", p.direccion);
        assertEquals(2470, p.totalCentavos());
        assertTrue(p.comprobante().contains("sin cobro registrado"));
    }
    @Test public void reintentoNoDuplicaPedidoNiDescuentaDosVeces() {
        String clave = UUID.randomUUID().toString();
        long id = db.reservar(cliente, producto, 2, clave);
        assertEquals(id, db.reservar(cliente, producto, 2, clave));
        assertEquals(3, stock()); assertEquals(1, db.listarPedidos(cliente, "").size());
    }
    @Test public void sobreventaYRolesInvalidosNoCambianStock() {
        for (long quien : new long[]{vendedor, -1}) {
            try { db.reservar(quien, producto, 1, UUID.randomUUID().toString()); fail(); }
            catch (IllegalArgumentException esperado) { }
        }
        try { reservar(6); fail(); } catch (IllegalArgumentException esperado) { }
        assertEquals(5, stock()); assertTrue(db.listarPedidos(cliente, "").isEmpty());
        assertEquals(-1, db.insertarPublicacion(new Publicacion(-1, cliente, "No permitido", 10, "", 1, false, null)));
        assertFalse(db.eliminarPublicacion(producto, cliente));
    }
    @Test public void cancelarDevuelveStockSoloUnaVezYProtegePropiedad() {
        long id = reservar(3); assertNull(db.obtenerPedido(id, otro));
        assertFalse(db.cancelarPedido(id, otro)); assertEquals(2, stock());
        assertTrue(db.cancelarPedido(id, cliente)); assertFalse(db.cancelarPedido(id, cliente));
        assertEquals(5, stock()); assertEquals(Pedido.CANCELADO, db.obtenerPedido(id, vendedor).estado);
    }
    @Test public void entregaValidaExigeCodigoYNoSePuedeReutilizar() {
        long id = reservar(2); Pedido p = db.obtenerPedido(id, cliente);
        assertFalse(db.entregarPedido(id, vendedor, "incorrecto"));
        assertFalse(db.entregarPedido(id, otro, p.token));
        assertTrue(db.entregarPedido(id, vendedor, QrUtil.PREFIJO + p.token));
        assertFalse(db.entregarPedido(id, vendedor, p.token));
        assertFalse(db.cancelarPedido(id, cliente)); assertEquals(3, stock());
        assertEquals(Pedido.ENTREGADO, db.obtenerPedido(id, cliente).estado);
    }
    @Test public void vencimientoLiberaStockYRechazaEntrega() {
        long id = reservar(5); Pedido p = db.obtenerPedido(id, cliente);
        db.getWritableDatabase().execSQL("UPDATE pedidos SET vence = 0 WHERE id = ?", new Object[]{id});
        assertFalse(db.entregarPedido(id, vendedor, p.token));
        assertEquals(Pedido.VENCIDO, db.obtenerPedido(id, cliente).estado);
        assertEquals(5, stock()); db.expirarPedidos(); assertEquals(5, stock());
    }
    @Test public void eliminarPublicacionConservaHistorialYNoLaRepublicaAlCancelar() {
        long id = reservar(1); assertTrue(db.eliminarPublicacion(producto, vendedor));
        assertNull(db.obtenerProducto(producto)); assertTrue(db.cancelarPedido(id, cliente));
        assertTrue(db.listarProductosDisponibles(-1, "").isEmpty());
        assertNotNull(db.obtenerPedido(id, cliente));
    }
    @Test public void noReservaSinDatosDeRecoleccion() {
        db.getWritableDatabase().execSQL("UPDATE usuarios SET direccion = '' WHERE id = ?", new Object[]{vendedor});
        try { reservar(1); fail(); } catch (IllegalArgumentException esperado) { }
        assertEquals(5, stock());
    }
    @Test public void dosClientesNoPuedenReservarLaUltimaUnidad() throws Exception {
        db.getWritableDatabase().execSQL("UPDATE publicaciones SET cantidad = 1 WHERE id = ?", new Object[]{producto});
        ExecutorService executor = Executors.newFixedThreadPool(2); CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<Boolean> uno = executor.submit(() -> intentarParalelo(cliente, inicio));
            Future<Boolean> dos = executor.submit(() -> intentarParalelo(otro, inicio));
            inicio.countDown(); assertNotEquals(uno.get(), dos.get()); assertEquals(0, stock());
            assertEquals(1, db.listarPedidos(vendedor, "").size());
        } finally { executor.shutdownNow(); }
    }
    private boolean intentarParalelo(long quien, CountDownLatch inicio) throws Exception {
        inicio.await();
        try (DBHelper independiente = new DBHelper(context, nombre)) {
            try { independiente.reservar(quien, producto, 1, UUID.randomUUID().toString()); return true; }
            catch (IllegalArgumentException e) { return false; }
        }
    }
    @Test public void qrSePuedeGenerarYLeerDesdeImagen() throws Exception {
        Pedido p = db.obtenerPedido(reservar(1), cliente);
        Bitmap qr = QrUtil.generar(p.token); File archivo = new File(context.getCacheDir(), "qr-" + UUID.randomUUID() + ".png");
        try {
            try (FileOutputStream salida = new FileOutputStream(archivo)) { qr.compress(Bitmap.CompressFormat.PNG, 100, salida); }
            assertEquals(QrUtil.PREFIJO + p.token, QrUtil.leer(context, Uri.fromFile(archivo)));
        } finally { qr.recycle(); archivo.delete(); }
    }
    @Test public void migracionVersionTresPreservaDatos() {
        String anterior = "prueba_migracion_" + UUID.randomUUID() + ".db";
        try {
            try (SQLiteDatabase vieja = context.openOrCreateDatabase(anterior, Context.MODE_PRIVATE, null)) {
                vieja.execSQL("CREATE TABLE usuarios (id INTEGER PRIMARY KEY, nombre TEXT, correo TEXT, password_hash TEXT, salt TEXT, rol TEXT, creado INTEGER)");
                vieja.execSQL("CREATE TABLE publicaciones (id INTEGER PRIMARY KEY, panaderia_id INTEGER, nombre TEXT, precio REAL, detalles TEXT, cantidad INTEGER, caja_sorpresa INTEGER, imagen TEXT, creado INTEGER)");
                vieja.execSQL("INSERT INTO usuarios VALUES (7, 'Negocio anterior', 'anterior@test.invalid', 'hash', 'sal', 'panaderia', 1)");
                vieja.execSQL("INSERT INTO publicaciones VALUES (9, 7, 'Pan anterior', 15.5, '', 4, 0, NULL, 1)");
                vieja.setVersion(3);
            }
            try (DBHelper migrada = new DBHelper(context, anterior)) {
                assertEquals(5, migrada.getReadableDatabase().getVersion());
                assertEquals("Negocio anterior", migrada.obtenerUsuario(7).nombre);
                assertEquals("Pan anterior", migrada.obtenerProducto(9).nombre);
                assertEquals(4, migrada.obtenerProducto(9).cantidad);
                assertTrue(migrada.listarPedidos(7, "").isEmpty());
            }
        } finally { context.deleteDatabase(anterior); }
    }
}
