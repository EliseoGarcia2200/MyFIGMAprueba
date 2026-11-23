package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class BuscarProductosActivity extends BaseActivity {

    /** Si se envía, la lista muestra solo los productos de esa panadería. */
    public static final String EXTRA_PANADERIA_ID = "panaderia_id";

    private DBHelper db;
    private ProductoAdapter adapter;
    private EditText etBuscar;
    private TextView tvSinProductos;
    private long panaderiaId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main4);

        db = new DBHelper(this);
        etBuscar = findViewById(R.id.etBuscarProducto);
        tvSinProductos = findViewById(R.id.tvSinProductos);

        RecyclerView rvProductos = findViewById(R.id.rvProductos);
        adapter = new ProductoAdapter(this::abrirDetalle);
        rvProductos.setLayoutManager(new LinearLayoutManager(this));
        rvProductos.setAdapter(adapter);

        // Si venimos de "Ver más" de una panadería, se indica en el buscador.
        panaderiaId = getIntent().getLongExtra(EXTRA_PANADERIA_ID, -1);
        if (panaderiaId > 0) {
            String nombre = db.obtenerNombreUsuario(panaderiaId);
            if (nombre != null) etBuscar.setHint("Buscar en " + nombre + "...");
        }

        alTocarReemplazar(R.id.tabLocal, BuscarLocalesActivity.class);
        alCambiarTexto(etBuscar, this::cargarLista);
    }

    /** Al volver del detalle (o de otra pantalla), la lista se refresca con las existencias actuales. */
    @Override
    protected void onResume() {
        super.onResume();
        cargarLista();
    }

    private void cargarLista() {
        String filtro = etBuscar.getText().toString().trim();
        List<Publicacion> productos = db.listarProductosDisponibles(panaderiaId, filtro);
        adapter.actualizar(productos);

        tvSinProductos.setVisibility(productos.isEmpty() ? View.VISIBLE : View.GONE);
        tvSinProductos.setText(filtro.isEmpty()
                ? "Aún no hay productos disponibles."
                : "No hay productos que coincidan con la búsqueda.");
    }

    private void abrirDetalle(Publicacion producto) {
        Intent intent = new Intent(this, DetalleProductoActivity.class);
        intent.putExtra(DetalleProductoActivity.EXTRA_ID, producto.id);
        startActivity(intent);
    }
}
