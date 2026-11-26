package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import java.util.UUID;

public class DetalleCompraActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main9);

        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        String producto = getIntent().getStringExtra("nombre_producto");
        String sucursal = getIntent().getStringExtra("sucursal");
        String precio = getIntent().getStringExtra("precio");
        if (producto == null) producto = "Caja Sorpresa de Pan Dulce";
        if (sucursal == null) sucursal = "Panadería Lupita";
        if (precio == null) precio = "$45.00";

        String finalProducto = producto;
        String finalSucursal = sucursal;
        String finalPrecio = precio;

        // Botón "Ver factura" -> abre la pantalla azul de factura (activity_main14)
        findViewById(R.id.btnVerFactura).setOnClickListener(v -> {
            String folioAleatorio = "PE-" + (10000 + (int) (Math.random() * 90000));
            Intent intent = new Intent(this, DetallePedidoActivity.class);
            intent.putExtra("folio", folioAleatorio);
            intent.putExtra("producto", finalProducto);
            intent.putExtra("sucursal", finalSucursal);
            intent.putExtra("precio", finalPrecio);
            startActivity(intent);
        });

        // Botón "Ver QR" -> abre QrActivity con un código aleatorio nuevo
        findViewById(R.id.btnVerQr).setOnClickListener(v -> {
            String qrAleatorio = "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Intent intent = new Intent(this, QrActivity.class);
            intent.putExtra("codigo_qr", qrAleatorio);
            startActivity(intent);
        });
    }
}