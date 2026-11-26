package com.example.myfigmaprueba;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MisPublicacionesActivity extends BaseActivity {

    private DBHelper db;
    private PublicacionAdapter adapter;
    private EditText etBuscar;
    private RecyclerView rvPublicaciones;
    private TextView tvSinPublicaciones;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        mostrarPantalla(R.layout.activity_main11);

        db = new DBHelper(this);
        etBuscar = findViewById(R.id.etBuscarPub);
        rvPublicaciones = findViewById(R.id.rvPublicaciones);
        tvSinPublicaciones = findViewById(R.id.tvSinPublicaciones);

        adapter = new PublicacionAdapter(this::abrirEdicion);
        rvPublicaciones.setLayoutManager(new LinearLayoutManager(this));
        rvPublicaciones.setAdapter(adapter);

        alTocar(R.id.fabAgregarPublicacion, EditarPublicacionActivity.class);

        // La lista se filtra mientras escribes en el buscador.
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                cargarLista();
            }
        });
    }

    /** Al volver de crear/editar, la lista se recarga con los cambios. */
    @Override
    protected void onResume() {
        super.onResume();
        cargarLista();
    }

    private void cargarLista() {
        if (db == null || isFinishing()) return;
        String filtro = etBuscar.getText().toString().trim();
        List<Publicacion> lista = db.listarPublicaciones(sesion.getId(), filtro);
        adapter.actualizar(lista);

        boolean vacia = lista.isEmpty();
        rvPublicaciones.setVisibility(vacia ? View.GONE : View.VISIBLE);
        tvSinPublicaciones.setVisibility(vacia ? View.VISIBLE : View.GONE);
        tvSinPublicaciones.setText(filtro.isEmpty()
                ? "Aún no tienes publicaciones.\nToca + para crear la primera."
                : "No hay publicaciones que coincidan con la búsqueda.");
    }

    private void abrirEdicion(Publicacion publicacion) {
        Intent intent = new Intent(this, EditarPublicacionActivity.class);
        intent.putExtra(EditarPublicacionActivity.EXTRA_ID, publicacion.id);
        startActivity(intent);
    }
    @Override protected void alActualizarTiempo() { cargarLista(); }
    @Override protected void onDestroy() { if (db != null) db.close(); super.onDestroy(); }
}
