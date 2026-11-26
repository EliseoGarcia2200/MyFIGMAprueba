package com.example.myfigmaprueba;

import android.os.Bundle;
import android.content.Intent;
import android.widget.LinearLayout;

public class CompraExitosaActivity extends ComprobanteActivity {
    private LinearLayout contenido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        contenido = Pantalla.abrir(this, "Reserva creada");
    }
    @Override protected void onResume() { super.onResume(); cargar(); }
    @Override protected void alActualizarTiempo() { cargar(); }
    private void cargar() {
        if (contenido == null || isFinishing()) return;
        try (DBHelper db = new DBHelper(this)) {
            Pedido p = db.obtenerPedido(getIntent().getLongExtra(Pedido.EXTRA_ID, -1), sesion.getId());
            if (p == null) { finish(); return; }
            contenido.removeAllViews();
            Pantalla.texto(contenido, p.folio(), true);
            Pantalla.texto(contenido, p.estadoTexto(), true);
            Pantalla.texto(contenido, p.producto + " · " + p.cantidad + " unidades\n" + p.panaderia
                    + "\nTotal: " + Dinero.formato(p.totalCentavos()), false);
            Pantalla.texto(contenido, "Paga en efectivo al recoger. La reserva dura dos horas.\n"
                    + "Recoge antes de " + Pedido.fecha(p.vence) + " y dentro del horario informado por la panadería.\n"
                    + p.direccion + "\nHorario: " + p.horario, false);
            Pantalla.boton(contenido, "Ver mi compra", v -> {
                startActivity(new Intent(this, DetalleCompraActivity.class).putExtra(Pedido.EXTRA_ID, p.id)); finish();
            });
            Pantalla.boton(contenido, "Descargar comprobante", v -> descargar(p));
            Pantalla.boton(contenido, "Seguir buscando", v -> irAInicioSegunRol());
        }
    }
}
