package com.example.myfigmaprueba;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class DireccionActivity extends BaseActivity {
    public static final String EXTRA_SELECCION = "seleccionar_ubicacion";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        boolean seleccion = getIntent().getBooleanExtra(EXTRA_SELECCION, false);
        if (seleccion && !sesion.esPanaderia()) { finish(); return; }
        LinearLayout panel = Pantalla.abrir(this, seleccion ? "Ubicación de la panadería" : "Punto de recolección");
        if (seleccion) {
            Pantalla.texto(panel, "Toca el mapa o escribe ambas coordenadas. Después guarda los datos de tu perfil.", false);
            int tipo = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL;
            EditText lat = Pantalla.campo(panel, "Latitud", getIntent().hasExtra("latitud") ? String.valueOf(getIntent().getDoubleExtra("latitud", 0)) : "", tipo);
            lat.setId(R.id.etMapaLatitud);
            EditText lon = Pantalla.campo(panel, "Longitud", getIntent().hasExtra("longitud") ? String.valueOf(getIntent().getDoubleExtra("longitud", 0)) : "", tipo);
            lon.setId(R.id.etMapaLongitud);
            MapasUtil.agregar(this, panel, mapa -> {
                LatLng inicial = new LatLng(19.0433, -98.2019);
                try { inicial = new LatLng(Double.parseDouble(lat.getText().toString()), Double.parseDouble(lon.getText().toString())); }
                catch (NumberFormatException ignored) { }
                mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(inicial, 14));
                if (!lat.getText().toString().isEmpty()) mapa.addMarker(new MarkerOptions().position(inicial));
                mapa.setOnMapClickListener(punto -> {
                    mapa.clear(); mapa.addMarker(new MarkerOptions().position(punto));
                    lat.setText(String.valueOf(punto.latitude)); lon.setText(String.valueOf(punto.longitude));
                });
            });
            Pantalla.boton(panel, "Usar estas coordenadas", v -> {
                try {
                    double latitud = Double.parseDouble(lat.getText().toString().replace(',', '.'));
                    double longitud = Double.parseDouble(lon.getText().toString().replace(',', '.'));
                    if (!Double.isFinite(latitud) || !Double.isFinite(longitud) || Math.abs(latitud) > 90 || Math.abs(longitud) > 180)
                        throw new NumberFormatException();
                    setResult(RESULT_OK, new Intent().putExtra("latitud", latitud).putExtra("longitud", longitud)); finish();
                } catch (NumberFormatException e) { lat.setError("Escribe ambas coordenadas válidas"); }
            });
        } else {
            try (DBHelper db = new DBHelper(this)) {
                Pedido p = db.obtenerPedido(getIntent().getLongExtra(Pedido.EXTRA_ID, -1), sesion.getId());
                if (p == null) { finish(); return; }
                Pantalla.texto(panel, p.panaderia, true);
                Pantalla.texto(panel, p.direccion + "\nHorario: " + p.horario + "\nReserva: " + p.folio(), false);
                if (p.latitud != null && p.longitud != null) MapasUtil.agregar(this, panel, mapa -> {
                    LatLng posicion = new LatLng(p.latitud, p.longitud);
                    mapa.addMarker(new MarkerOptions().position(posicion).title(p.panaderia));
                    mapa.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 16));
                });
                Pantalla.boton(panel, "Abrir indicaciones", v -> {
                    String destino = p.latitud != null ? p.latitud + "," + p.longitud : p.direccion;
                    Intent mapa = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destino)));
                    try { startActivity(mapa); } catch (android.content.ActivityNotFoundException e) {
                        Toast.makeText(this, "No hay una aplicación para abrir el mapa", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }
}
