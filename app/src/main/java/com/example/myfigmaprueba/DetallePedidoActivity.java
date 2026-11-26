package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.UUID;

public class DetallePedidoActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main14);

        String folio = getIntent().getStringExtra("folio");
        String precio = getIntent().getStringExtra("precio");

        TextView tvNumeroPedido = findViewById(R.id.tvNumeroPedido);
        TextView tvTotalCobrar = findViewById(R.id.tvTotalCobrar);
        View btnEscanearQr = findViewById(R.id.btnEscanearQr);

        if (tvNumeroPedido != null && folio != null) {
            tvNumeroPedido.setText("Pedido #" + folio);
        }
        if (tvTotalCobrar != null && precio != null) {
            tvTotalCobrar.setText(precio);
        }

        if (btnEscanearQr != null) {
            btnEscanearQr.setOnClickListener(v -> {
                String qrAleatorio = "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                Intent intent = new Intent(this, QrActivity.class);
                intent.putExtra("codigo_qr", qrAleatorio);
                startActivity(intent);
            });
        }
    }
}