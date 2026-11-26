package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

public class MisComprasActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main8);

        ImageView btnBack = findViewById(R.id.btnBack);
        EditText etBuscar = findViewById(R.id.etBuscarCompras);
        View cardCompraDemo = findViewById(R.id.cardCompra1);
        View btnVerDetalles = findViewById(R.id.btnVerDetallesCompra1);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        View.OnClickListener abrirDetalle = v -> {
            Intent intent = new Intent(this, DetalleCompraActivity.class);
            intent.putExtra("nombre_producto", "Caja Sorpresa de Pan Dulce");
            intent.putExtra("sucursal", "Panadería Lupita");
            intent.putExtra("precio", "$45.00");
            intent.putExtra("codigo_qr", "ORD-DEMO-1024");
            startActivity(intent);
        };

        if (btnVerDetalles != null) btnVerDetalles.setOnClickListener(abrirDetalle);
        if (cardCompraDemo != null) cardCompraDemo.setOnClickListener(abrirDetalle);

        if (etBuscar != null && cardCompraDemo != null) {
            etBuscar.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String q = s.toString().trim().toLowerCase();
                    boolean coincide = q.isEmpty()
                            || "caja sorpresa de pan dulce".contains(q)
                            || "panaderia lupita".contains(q)
                            || "pan".contains(q);
                    cardCompraDemo.setVisibility(coincide ? View.VISIBLE : View.GONE);
                }
            });
        }
    }
}