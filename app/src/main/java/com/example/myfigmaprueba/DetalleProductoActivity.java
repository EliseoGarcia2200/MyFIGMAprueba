package com.example.myfigmaprueba;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import java.util.UUID;

public class DetalleProductoActivity extends BaseActivity {

    /** Id del producto (publicación) que se muestra. */
    public static final String EXTRA_ID = "producto_id";
    private String solicitud;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        solicitud = savedInstanceState == null ? UUID.randomUUID().toString() : savedInstanceState.getString("solicitud");
        if (solicitud == null) solicitud = UUID.randomUUID().toString();
        mostrarPantalla(R.layout.activity_main5);

        long id = getIntent().getLongExtra(EXTRA_ID, -1);
        Publicacion producto;
        try (DBHelper db = new DBHelper(this)) { producto = db.obtenerProducto(id); }
        if (producto == null || producto.cantidad < 1) {
            Toast.makeText(this, "Este producto ya no está disponible", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvTituloTop)).setText(producto.nombre);
        ((TextView) findViewById(R.id.tvNombreProducto)).setText(producto.nombre);
        ((TextView) findViewById(R.id.tvSucursal)).setText("Sucursal: " + producto.panaderiaNombre);

        // Solo hay un precio por ahora, así que se oculta el precio anterior tachado.
        findViewById(R.id.tvPrecioAnterior).setVisibility(View.GONE);
        findViewById(R.id.tvFlecha).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.tvPrecioActual))
                .setText(PublicacionAdapter.formatoPrecio(producto.precio));

        ((TextView) findViewById(R.id.tvDescripcionProducto)).setText(
                producto.detalles.isEmpty() ? "Sin descripción." : producto.detalles);
        ((TextView) findViewById(R.id.tvDescripcionProveedor)).setText(
                producto.panaderiaNombre + "\nDisponibles: " + producto.cantidad
                        + (producto.cajaSorpresa ? "\nIncluye caja sorpresa." : ""));

        Bitmap foto = ImagenUtil.cargar(producto.imagen, 600);
        if (foto != null) {
            ImageView imagen = findViewById(R.id.imgProducto);
            imagen.setPadding(0, 0, 0, 0);
            imagen.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imagen.setImageBitmap(foto);
        }

        Button comprar = findViewById(R.id.btnComprar);
        comprar.setText(sesion.esPanaderia() ? "Vista previa del producto" : "Reservar y pagar al recoger");
        comprar.setEnabled(!sesion.esPanaderia());
        comprar.setOnClickListener(v -> confirmarReserva(id));
    }

    private void confirmarReserva(long id) {
        Publicacion producto;
        Negocio negocio;
        try (DBHelper db = new DBHelper(this)) {
            producto = db.obtenerProducto(id);
            negocio = producto == null ? null : db.obtenerNegocio(producto.panaderiaId);
        }
        if (producto == null || producto.cantidad < 1) {
            Toast.makeText(this, "Producto agotado o eliminado", Toast.LENGTH_LONG).show(); return;
        }
        if (negocio == null || !negocio.permiteReservas()) {
            Toast.makeText(this, "La panadería aún debe completar su dirección y horario de recolección", Toast.LENGTH_LONG).show(); return;
        }
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        int margen = Pantalla.dp(panel, 20); panel.setPadding(margen, margen, margen, margen);
        Pantalla.texto(panel, negocio.nombre + "\n" + negocio.direccion + "\nHorario: " + negocio.horario
                + "\nDisponibles: " + producto.cantidad + "\nPrecio unitario: " + Dinero.formato(Dinero.centavos(producto.precio)), false);
        EditText cantidad = Pantalla.campo(panel, "Cantidad", "1", InputType.TYPE_CLASS_NUMBER);
        TextView total = Pantalla.texto(panel, "", true);
        alCambiarTexto(cantidad, () -> actualizarTotal(total, cantidad, producto));
        actualizarTotal(total, cantidad, producto);
        Pantalla.texto(panel, "Reserva por dos horas. Paga en efectivo al recoger dentro del horario de la panadería. Si vence o cancelas, las unidades se liberan.", false);
        AlertDialog dialogo = new AlertDialog.Builder(this).setTitle("Reservar " + producto.nombre)
                .setView(panel).setNegativeButton("Volver", null).setPositiveButton("Confirmar reserva", null).create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int unidades;
            try {
                unidades = Integer.parseInt(cantidad.getText().toString());
                if (unidades < 1 || unidades > producto.cantidad || unidades > 10000) throw new NumberFormatException();
            } catch (NumberFormatException e) { cantidad.setError("Elige una cantidad entre 1 y " + Math.min(10000, producto.cantidad)); return; }
            v.setEnabled(false); dialogo.setCancelable(false);
            dialogo.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
            long clienteId = sesion.getId(); String clave = solicitud;
            new Thread(() -> {
                long pedidoId = -1; String error = null;
                try (DBHelper db = new DBHelper(getApplicationContext())) { pedidoId = db.reservar(clienteId, id, unidades, clave); }
                catch (IllegalArgumentException e) { error = e.getMessage(); }
                catch (RuntimeException e) { error = "No se pudo guardar la reserva. Intenta de nuevo."; }
                long resultado = pedidoId; String mensaje = error;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    v.setEnabled(true); dialogo.setCancelable(true); dialogo.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                    if (resultado < 0) { Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show(); return; }
                    dialogo.dismiss();
                    startActivity(new Intent(this, CompraExitosaActivity.class).putExtra(Pedido.EXTRA_ID, resultado)); finish();
                });
            }, "reservar-producto").start();
        }));
        dialogo.show();
    }

    private void actualizarTotal(TextView total, EditText cantidad, Publicacion p) {
        try {
            int n = Integer.parseInt(cantidad.getText().toString());
            total.setText(n > 0 && n <= 10000 ? "Total: " + Dinero.formato(Math.multiplyExact(Dinero.centavos(p.precio), n)) : "Cantidad no válida");
        } catch (NumberFormatException | ArithmeticException e) { total.setText("Escribe una cantidad"); }
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("solicitud", solicitud); }
}
