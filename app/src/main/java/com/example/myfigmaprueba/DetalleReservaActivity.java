package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

public abstract class DetalleReservaActivity extends ComprobanteActivity {
    private LinearLayout contenido;
    private long id;
    private final ActivityResultLauncher<PickVisualMediaRequest> lector = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                android.content.Context app = getApplicationContext();
                new Thread(() -> {
                    String resultado = null;
                    try { resultado = QrUtil.leer(app, uri); } catch (Exception ignored) { }
                    String codigo = resultado;
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        if (codigo == null || !codigo.startsWith(QrUtil.PREFIJO)) {
                            Toast.makeText(this, "No se encontró un QR de Cero Desperdicio. Puedes ingresar el código.", Toast.LENGTH_LONG).show();
                        } else confirmarEntrega(codigo);
                    });
                }, "leer-qr").start();
            });

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado); if (!verificarAcceso()) return;
        contenido = Pantalla.abrir(this, sesion.esPanaderia() ? "Detalle del pedido" : "Detalle de compra");
        id = getIntent().getLongExtra(Pedido.EXTRA_ID, -1);
    }
    @Override protected void onResume() { super.onResume(); cargar(); }
    @Override protected void alActualizarTiempo() { cargar(); }
    private void cargar() {
        if (contenido == null || isFinishing()) return;
        try (DBHelper db = new DBHelper(this)) {
            Pedido p = db.obtenerPedido(id, sesion.getId());
            if (p == null) { Toast.makeText(this, "No se encontró el pedido", Toast.LENGTH_SHORT).show(); finish(); return; }
            contenido.removeAllViews();
            Pantalla.texto(contenido, p.folio() + " · " + p.estadoTexto(), true);
            Pantalla.texto(contenido, p.producto, true);
            if (p.imagen != null) {
                ImageView foto = new ImageView(this); foto.setContentDescription(p.producto);
                contenido.addView(foto, new LinearLayout.LayoutParams(-1, Pantalla.dp(contenido, 180)));
                Miniaturas.mostrar(foto, p.imagen, 600);
            }
            Pantalla.texto(contenido, "Cantidad: " + p.cantidad + "\nPrecio unitario: " + Dinero.formato(p.precioCentavos)
                    + "\nTotal: " + Dinero.formato(p.totalCentavos()) + "\n" + p.detalles, false);
            LinearLayout resumen = Pantalla.tarjeta(contenido);
            Pantalla.texto(resumen, "Recolección", true);
            Pantalla.texto(resumen, p.panaderia + "\n" + p.direccion + "\nHorario: " + p.horario
                    + "\nLímite de reserva: " + Pedido.fecha(p.vence), false);
            Pantalla.texto(contenido, "Creado: " + Pedido.fecha(p.creado) + "\nPago en efectivo al recoger."
                    + (Pedido.ENTREGADO.equals(p.estado) ? " Cobro registrado por la panadería." : " Sin cobro registrado."), false);
            if (sesion.esPanaderia()) Pantalla.texto(contenido, "Cliente: " + p.cliente + "\n" + p.correoCliente, false);
            Pantalla.boton(contenido, "Ver ubicación", v -> startActivity(new Intent(this, DireccionActivity.class).putExtra(Pedido.EXTRA_ID, id)));
            Pantalla.boton(contenido, "Descargar comprobante", v -> descargar(p));
            if (p.activo()) {
                if (sesion.esPanaderia()) {
                    Pantalla.boton(contenido, "Confirmar entrega y cobro con código", v -> confirmarEntrega(""));
                    Pantalla.boton(contenido, "Leer QR desde una imagen", v -> lector.launch(new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
                } else Pantalla.boton(contenido, "Mostrar QR de recolección", v -> startActivity(new Intent(this, QrActivity.class).putExtra(Pedido.EXTRA_ID, id)));
                Pantalla.boton(contenido, "Cancelar reserva", v -> new AlertDialog.Builder(this)
                        .setTitle("Cancelar " + p.folio()).setMessage("Las unidades volverán a estar disponibles. No se ha realizado ningún cobro.")
                        .setNegativeButton("Volver", null).setPositiveButton("Cancelar reserva", (d, w) -> {
                            try (DBHelper datos = new DBHelper(this)) {
                                boolean ok = datos.cancelarPedido(id, sesion.getId());
                                Toast.makeText(this, ok ? "Reserva cancelada" : "La reserva ya no está activa", Toast.LENGTH_SHORT).show();
                            }
                            cargar();
                        }).show());
            }
        }
    }

    private void confirmarEntrega(String valor) {
        LinearLayout panel = new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL);
        int margen = Pantalla.dp(panel, 20); panel.setPadding(margen, margen, margen, margen);
        Pantalla.texto(panel, "Verifica el producto y recibe el efectivo antes de confirmar. Pide al cliente su código de recolección.", false);
        EditText codigo = Pantalla.campo(panel, "Código de recolección", valor, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        AlertDialog dialogo = new AlertDialog.Builder(this).setTitle("Confirmar entrega y cobro")
                .setView(panel).setNegativeButton("Volver", null).setPositiveButton("Confirmar", null).create();
        dialogo.setOnShowListener(d -> dialogo.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try (DBHelper db = new DBHelper(this)) {
                if (!db.entregarPedido(id, sesion.getId(), codigo.getText().toString())) {
                    codigo.setError("Código incorrecto o reserva inactiva"); cargar(); return;
                }
            }
            dialogo.dismiss(); Toast.makeText(this, "Entrega y cobro registrados", Toast.LENGTH_SHORT).show(); cargar();
        }));
        dialogo.show();
    }
}
