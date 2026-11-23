package com.example.myfigmaprueba;

import android.os.Bundle;

public class DetalleCompraActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main9);
        alTocar(R.id.btnVerQr, QrActivity.class);
        // TODO (paso 4): conectar btnVerFactura.
    }
}
