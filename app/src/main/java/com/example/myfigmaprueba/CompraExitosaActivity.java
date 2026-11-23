package com.example.myfigmaprueba;

import android.os.Bundle;

public class CompraExitosaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main15);
        alTocar(R.id.btnCerrarExito, InicioPanaderiaActivity.class);
        // TODO (paso 4): conectar btnVerPedido y btnDescargarComprobante.
    }
}
