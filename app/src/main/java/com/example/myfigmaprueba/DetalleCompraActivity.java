package com.example.myfigmaprueba;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class DetalleCompraActivity extends BaseActivity {

    private long pedidoId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        mostrarPantalla(R.layout.activity_main9);

        pedidoId = getIntent().getLongExtra(Pedido.EXTRA_ID, -1);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        cargarDatosReales();
    }

    private void cargarDatosReales() {
        try (DBHelper db = new DBHelper(this)) {
            Pedido p = db.obtenerPedido(pedidoId, sesion.getId());
            Usuario u = db.obtenerUsuario(sesion.getId());
            if (p == null) {
                finish();
                return;
            }

            TextView tvNombre = findViewById(R.id.tvNombreProdCompra);
            TextView tvPrecio = findViewById(R.id.tvPrecioProdCompra);

            if (tvNombre != null) tvNombre.setText(p.producto);
            if (tvPrecio != null) tvPrecio.setText(Dinero.formato(p.totalCentavos()));

            View btnFactura = findViewById(R.id.btnVerFactura);
            if (btnFactura != null) {
                btnFactura.setOnClickListener(v -> mostrarFacturaFigma(p, u));
            }

            View btnQr = findViewById(R.id.btnVerQr);
            if (btnQr != null) {
                btnQr.setOnClickListener(v -> abrirQr(p.id));
            }
        }
    }

    private void mostrarFacturaFigma(Pedido p, Usuario u) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Light_NoActionBar);
        dialog.setContentView(R.layout.activity_main14);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        View btnBackFactura = dialog.findViewById(R.id.btnBackFactura);
        TextView tvNumero = dialog.findViewById(R.id.tvNumeroPedido);
        TextView tvBadge = dialog.findViewById(R.id.tvBadgeEstado);
        TextView tvTotal = dialog.findViewById(R.id.tvTotalCobrar);
        TextView tvLimite = dialog.findViewById(R.id.tvFechaLimite);
        TextView tvFechaCompra = dialog.findViewById(R.id.tvFechaCompra);
        TextView tvEstadoActual = dialog.findViewById(R.id.tvEstadoActual);
        TextView tvNombreUsuario = dialog.findViewById(R.id.tvNombreUsuario);
        TextView tvCorreoUsuario = dialog.findViewById(R.id.tvCorreoUsuario);
        TextView tvDireccion = dialog.findViewById(R.id.tvDireccionFactura);
        TextView tvProducto = dialog.findViewById(R.id.tvProductoFactura);
        TextView tvCantidad = dialog.findViewById(R.id.tvCantidadFactura);
        TextView tvPrecioUnit = dialog.findViewById(R.id.tvPrecioUnitarioFactura);
        ImageView imgQrPreview = dialog.findViewById(R.id.imgQrPreviewFactura);
        View btnQr = dialog.findViewById(R.id.btnEscanearQr);

        if (btnBackFactura != null) btnBackFactura.setOnClickListener(v -> dialog.dismiss());
        if (tvNumero != null) tvNumero.setText("Pedido #" + p.folio());
        if (tvBadge != null) tvBadge.setText(p.estadoTexto());
        if (tvTotal != null) tvTotal.setText(Dinero.formato(p.totalCentavos()));
        if (tvLimite != null) tvLimite.setText(Pedido.fecha(p.vence));
        if (tvFechaCompra != null) tvFechaCompra.setText(Pedido.fecha(p.vence));
        if (tvEstadoActual != null) tvEstadoActual.setText(p.estadoTexto());

        if (u != null) {
            if (tvNombreUsuario != null) tvNombreUsuario.setText(u.nombre);
            if (tvCorreoUsuario != null) tvCorreoUsuario.setText(u.correo);
        }
        if (tvDireccion != null) tvDireccion.setText(p.direccion);
        if (tvProducto != null) tvProducto.setText(p.producto);
        if (tvCantidad != null) tvCantidad.setText("Cant.: " + p.cantidad + " unidad(es)");
        if (tvPrecioUnit != null) tvPrecioUnit.setText("Total: " + Dinero.formato(p.totalCentavos()));

        if (imgQrPreview != null) {
            try {
                imgQrPreview.setImageBitmap(QrUtil.generar(p.token));
            } catch (Exception ignored) { }
        }

        if (btnQr != null) {
            btnQr.setOnClickListener(v -> {
                dialog.dismiss();
                abrirQr(p.id);
            });
        }

        dialog.show();
    }

    private void abrirQr(long id) {
        Intent intent = new Intent(this, QrActivity.class);
        intent.putExtra(Pedido.EXTRA_ID, id);
        startActivity(intent);
    }
}