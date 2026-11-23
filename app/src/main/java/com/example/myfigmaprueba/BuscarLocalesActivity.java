package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BuscarLocalesActivity extends BaseActivity {

    private DBHelper db;
    private LocalAdapter adapter;
    private EditText etBuscar;
    private TextView tvSinLocales;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main3);

        db = new DBHelper(this);
        etBuscar = findViewById(R.id.etBuscarMapa);
        tvSinLocales = findViewById(R.id.tvSinLocales);

        RecyclerView rvLocales = findViewById(R.id.rvLocales);
        adapter = new LocalAdapter(this::abrirProductosDe);
        rvLocales.setLayoutManager(new LinearLayoutManager(this));
        rvLocales.setAdapter(adapter);

        alTocarReemplazar(R.id.tabProducto, BuscarProductosActivity.class);
        alCambiarTexto(etBuscar, this::cargarLista);
        // TODO (paso 4C): mostrar las panaderías como marcadores en el mapa (mapBusqueda).
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarLista();
    }

    private void cargarLista() {
        String filtro = etBuscar.getText().toString().trim();
        List<Local> locales = db.listarLocales(filtro);
        adapter.actualizar(locales);

        tvSinLocales.setVisibility(locales.isEmpty() ? View.VISIBLE : View.GONE);
        tvSinLocales.setText(filtro.isEmpty()
                ? "Aún no hay panaderías con excedentes."
                : "No hay panaderías que coincidan con la búsqueda.");
    }

    /** "Ver más" abre la lista de productos pero solo de esa panadería. */
    private void abrirProductosDe(Local local) {
        Intent intent = new Intent(this, BuscarProductosActivity.class);
        intent.putExtra(BuscarProductosActivity.EXTRA_PANADERIA_ID, local.id);
        startActivity(intent);
    }
}
