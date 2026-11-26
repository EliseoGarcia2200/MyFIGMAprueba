package com.example.myfigmaprueba;

import android.os.Bundle;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.ImageView;

public class QrActivity extends BaseActivity {
    private LinearLayout contenido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        contenido = Pantalla.abrir(this, "Código de recolección");
    }
    @Override protected void onResume() { super.onResume(); cargar(); }
    @Override protected void alActualizarTiempo() { cargar(); }
    private void cargar() {
        if (contenido == null || isFinishing()) return;
        try (DBHelper db = new DBHelper(this)) {
            Pedido p = db.obtenerPedido(getIntent().getLongExtra(Pedido.EXTRA_ID, -1), sesion.getId());
            contenido.removeAllViews();
            if (p == null || p.clienteId != sesion.getId()) { finish(); return; }
            Pantalla.texto(contenido, p.folio() + " · " + p.producto, true);
            if (!p.activo()) { Pantalla.texto(contenido, p.estadoTexto() + ". Este código ya no se puede utilizar.", false); return; }
            Pantalla.texto(contenido, "Muestra este QR en " + p.panaderia + " al recoger y pagar tu reserva.", false);
            try {
                ImageView qr = new ImageView(this); qr.setContentDescription("QR de recolección de " + p.folio());
                qr.setImageBitmap(QrUtil.generar(p.token));
                contenido.addView(qr, new LinearLayout.LayoutParams(-1, Pantalla.dp(contenido, 280)));
            } catch (Exception e) { Pantalla.texto(contenido, "Usa el código de texto para recoger tu pedido.", false); }
            Pantalla.texto(contenido, "Código: " + p.token + "\nVálido hasta: " + Pedido.fecha(p.vence), true);
            Pantalla.texto(contenido, "Total a pagar en efectivo: " + Dinero.formato(p.totalCentavos())
                    + "\n" + p.direccion + "\nHorario: " + p.horario, false);
            Pantalla.boton(contenido, "Ver ubicación", v -> startActivity(new Intent(this, DireccionActivity.class).putExtra(Pedido.EXTRA_ID, p.id)));
        }
    }
}
