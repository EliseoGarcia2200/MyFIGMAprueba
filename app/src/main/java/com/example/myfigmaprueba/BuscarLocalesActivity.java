package com.example.myfigmaprueba;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BuscarLocalesActivity extends BaseActivity implements OnMapReadyCallback {

    private DBHelper db;
    private LocalAdapter adapter;
    private EditText etBuscar;
    private ImageView btnLupa, btnLimpiar;
    private ListView lvSugerencias;
    private TextView tvSinLocales;
    private GoogleMap mMap;
    private Marker marcadorBusqueda;

    private final List<Address> resultadosMapa = new ArrayList<>();
    private final List<String> textosSugerencias = new ArrayList<>();
    private ArrayAdapter<String> sugerenciasAdapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable tareaBusqueda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main3);

        db = new DBHelper(this);
        etBuscar = findViewById(R.id.etBuscarMapa);
        btnLupa = findViewById(R.id.btnBuscarLupa);
        btnLimpiar = findViewById(R.id.btnLimpiarBusqueda);
        lvSugerencias = findViewById(R.id.lvSugerenciasMapa);
        tvSinLocales = findViewById(R.id.tvSinLocales);

        RecyclerView rvLocales = findViewById(R.id.rvLocales);
        adapter = new LocalAdapter(this::abrirProductosDe);
        rvLocales.setLayoutManager(new LinearLayoutManager(this));
        rvLocales.setAdapter(adapter);

        sugerenciasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, textosSugerencias);
        lvSugerencias.setAdapter(sugerenciasAdapter);

        alTocarReemplazar(R.id.tabProducto, BuscarProductosActivity.class);

        // Botón X: borra texto y oculta sugerencias
        btnLimpiar.setOnClickListener(v -> {
            etBuscar.setText("");
            lvSugerencias.setVisibility(View.GONE);
            cargarLista();
        });

        // Botón Lupa: busca y mueve el mapa al primer resultado
        btnLupa.setOnClickListener(v -> ejecutarBusquedaDirecta());

        // Tecla "Buscar" del teclado
        etBuscar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                ejecutarBusquedaDirecta();
                return true;
            }
            return false;
        });

        // Al tocar una vista previa de la lista
        lvSugerencias.setOnItemClickListener((parent, view, position, id) -> {
            if (position < resultadosMapa.size()) {
                Address lugar = resultadosMapa.get(position);
                moverMapaALugar(lugar);
                lvSugerencias.setVisibility(View.GONE);
                ocultarTeclado();
            }
        });

        // Al escribir: filtra panaderías, muestra/oculta la X y busca sugerencias en el mapa
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String texto = s.toString().trim();
                btnLimpiar.setVisibility(texto.isEmpty() ? View.GONE : View.VISIBLE);
                cargarLista();

                if (tareaBusqueda != null) handler.removeCallbacks(tareaBusqueda);

                if (texto.length() < 3) {
                    lvSugerencias.setVisibility(View.GONE);
                    return;
                }

                tareaBusqueda = () -> buscarSugerenciasEnMapa(texto, false);
                handler.postDelayed(tareaBusqueda, 400);
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapBusqueda);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void ejecutarBusquedaDirecta() {
        String texto = etBuscar.getText().toString().trim();
        ocultarTeclado();
        if (texto.isEmpty()) return;
        buscarSugerenciasEnMapa(texto, true);
    }

    private void buscarSugerenciasEnMapa(String query, boolean irAlPrimero) {
        executor.execute(() -> {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> direcciones = geocoder.getFromLocationName(query + ", Puebla, Mexico", 5);
                handler.post(() -> {
                    resultadosMapa.clear();
                    textosSugerencias.clear();

                    if (direcciones != null && !direcciones.isEmpty()) {
                        resultadosMapa.addAll(direcciones);
                        for (Address dir : direcciones) {
                            String linea = dir.getAddressLine(0);
                            textosSugerencias.add(linea != null ? linea : query);
                        }
                        sugerenciasAdapter.notifyDataSetChanged();

                        if (irAlPrimero) {
                            moverMapaALugar(resultadosMapa.get(0));
                            lvSugerencias.setVisibility(View.GONE);
                        } else {
                            lvSugerencias.setVisibility(View.VISIBLE);
                        }
                    } else {
                        lvSugerencias.setVisibility(View.GONE);
                        if (irAlPrimero) {
                            Toast.makeText(this, "No se encontró ese lugar", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (IOException e) {
                handler.post(() -> lvSugerencias.setVisibility(View.GONE));
            }
        });
    }

    private void moverMapaALugar(Address lugar) {
        if (mMap == null) return;
        LatLng pos = new LatLng(lugar.getLatitude(), lugar.getLongitude());
        if (marcadorBusqueda != null) marcadorBusqueda.remove();

        String titulo = lugar.getFeatureName() != null ? lugar.getFeatureName() : "Ubicación encontrada";
        marcadorBusqueda = mMap.addMarker(new MarkerOptions().position(pos).title(titulo));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
    }

    private void ocultarTeclado() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng puebla = new LatLng(19.0005, -98.2020);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(puebla, 15f));

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);

        mMap.addMarker(new MarkerOptions()
                .position(puebla)
                .title("Panadería Lupita")
                .snippet("Con excedentes"));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(19.0040, -98.1980))
                .title("Panadería Esperanza")
                .snippet("Con excedentes"));
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

    private void abrirProductosDe(Local local) {
        Intent intent = new Intent(this, BuscarProductosActivity.class);
        intent.putExtra(BuscarProductosActivity.EXTRA_PANADERIA_ID, local.id);
        startActivity(intent);
    }
}