package com.example.myfigmaprueba;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.EditText;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.UUID;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FlujoCompraTest {
    private Context context;
    private DBHelper db;
    private long vendedor, cliente, producto;
    private String correoVendedor, nombreProducto;
    private Usuario sesionAnterior;

    @Before public void preparar() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext(); db = new DBHelper(context);
        sesionAnterior = db.obtenerUsuario(new SesionManager(context).getId());
        String sufijo = UUID.randomUUID().toString(); correoVendedor = "ui-pan-" + sufijo + "@test.invalid";
        vendedor = db.registrarUsuario("Panadería de prueba", correoVendedor, "clave123", Usuario.ROL_PANADERIA);
        cliente = db.registrarUsuario("Cliente de prueba", "ui-cliente-" + sufijo + "@test.invalid", "clave123", Usuario.ROL_CLIENTE);
        db.guardarNegocio(vendedor, "Calle de prueba 123", "Todos los días 08:00 a 22:00", "", "", null, null);
        nombreProducto = "Pan de prueba " + sufijo.substring(0, 6);
        producto = db.insertarPublicacion(new Publicacion(-1, vendedor, nombreProducto, 12.35, "Pan recién horneado", 5, false, null));
        new SesionManager(context).iniciar(db.obtenerUsuario(cliente));
    }

    @After public void limpiar() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            for (Stage etapa : new Stage[]{Stage.RESUMED, Stage.STARTED, Stage.STOPPED, Stage.CREATED}) {
                for (Activity a : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(etapa))) a.finish();
            }
        });
        limpiarDatos();
        if (sesionAnterior == null) new SesionManager(context).cerrar();
        else new SesionManager(context).iniciar(sesionAnterior);
        db.close();
    }
    private void limpiarDatos() {
        db.getWritableDatabase().delete("pedidos", "cliente_id = ?", new String[]{String.valueOf(cliente)});
        db.getWritableDatabase().delete("publicaciones", "panaderia_id = ?", new String[]{String.valueOf(vendedor)});
        db.getWritableDatabase().delete("usuarios", "id IN (?, ?)", new String[]{String.valueOf(vendedor), String.valueOf(cliente)});
    }

    @Test public void clienteReservaYPanaderiaEntregaConCodigo() throws Exception {
        try (ActivityScenario<BuscarProductosActivity> inicio = ActivityScenario.launch(BuscarProductosActivity.class)) {
            onView(withText(nombreProducto)).perform(click());
            onView(withId(R.id.btnComprar)).perform(click());
            onView(isAssignableFrom(EditText.class)).perform(replaceText("2"), closeSoftKeyboard());
            onView(withText("Confirmar reserva")).perform(click());
            esperarTexto("Reserva creada");
            onView(withText("Ver mi compra")).perform(scrollTo(), click());
            onView(withText("Mostrar QR de recolección")).perform(scrollTo(), click());
            esperarTexto("Código de recolección");
            Pedido p = db.listarPedidos(cliente, "").get(0);
            assertEquals(2470, p.totalCentavos()); assertEquals(3, db.obtenerProducto(producto).cantidad);
            captura("qa-qr.png");
            onView(withId(R.id.navPerfil)).perform(click());
            onView(withText("Cerrar sesión")).perform(scrollTo(), click());
            onView(withId(R.id.etCorreo)).perform(replaceText(correoVendedor));
            onView(withId(R.id.etPassword)).perform(replaceText("clave123"), closeSoftKeyboard());
            onView(withId(R.id.btnIniciarSesion)).perform(click());
            onView(withId(R.id.navCompras)).perform(click());
            onView(withText(nombreProducto + " · 2 unidades")).perform(click());
            onView(withText("Confirmar entrega y cobro con código")).perform(scrollTo(), click());
            onView(withHint("Código de recolección")).perform(replaceText(p.token), closeSoftKeyboard());
            onView(withText("Confirmar")).perform(click());
            esperarTexto(p.folio() + " · Entregado y cobrado");
            assertEquals(Pedido.ENTREGADO, db.obtenerPedido(p.id, cliente).estado);
            captura("qa-entrega.png");
        }
    }

    @Test public void clienteNoPuedeAbrirEditorDePanaderia() {
        try (ActivityScenario<BuscarProductosActivity> intento = ActivityScenario.launch(BuscarProductosActivity.class)) {
            intento.onActivity(a -> a.startActivity(new Intent(a, EditarPublicacionActivity.class)));
            esperarTexto("Local");
            assertTrue(db.listarPublicaciones(cliente, "").isEmpty());
        }
    }

    private void esperarTexto(String texto) {
        long limite = System.currentTimeMillis() + 15000;
        do {
            try { onView(withText(texto)).check(matches(isDisplayed())); return; }
            catch (AssertionError | androidx.test.espresso.NoMatchingViewException e) {
                if (System.currentTimeMillis() >= limite) throw e;
                try { Thread.sleep(100); } catch (InterruptedException interrumpido) { Thread.currentThread().interrupt(); throw new AssertionError(interrumpido); }
            }
        } while (true);
    }
    private void captura(String nombre) throws Exception {
        Bitmap imagen = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        if (imagen != null) {
            try (FileOutputStream salida = new FileOutputStream(new File(context.getExternalFilesDir(null), nombre))) {
                imagen.compress(Bitmap.CompressFormat.PNG, 100, salida);
            } finally { imagen.recycle(); }
        }
    }
}
