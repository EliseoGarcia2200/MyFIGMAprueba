package com.example.myfigmaprueba;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.UUID;

public class QrActivity extends BaseActivity implements OnMapReadyCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarPantalla(R.layout.activity_main7);

        ImageView btnBack = findViewById(R.id.btnBack);
        ImageView imgCodigoQr = findViewById(R.id.imgCodigoQr);
        TextView tvVigenciaQr = findViewById(R.id.tvVigenciaQr);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Si no viene un código, genera uno aleatorio automáticamente
        String codigoQr = getIntent().getStringExtra("codigo_qr");
        if (codigoQr == null || codigoQr.isEmpty()) {
            codigoQr = "QR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        if (tvVigenciaQr != null) {
            tvVigenciaQr.setText("Código: " + codigoQr + " • Válido hoy");
        }

        Bitmap qrBitmap = generarQrBitmap(codigoQr, 500, 500);
        if (qrBitmap != null && imgCodigoQr != null) {
            imgCodigoQr.setImageBitmap(qrBitmap);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapQr);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private Bitmap generarQrBitmap(String texto, int ancho, int alto) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(texto, BarcodeFormat.QR_CODE, ancho, alto);
            Bitmap bmp = Bitmap.createBitmap(ancho, alto, Bitmap.Config.RGB_565);
            for (int x = 0; x < ancho; x++) {
                for (int y = 0; y < alto; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) {
            return null;
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        LatLng puntoEntrega = new LatLng(19.0005, -98.2020);
        googleMap.addMarker(new MarkerOptions()
                .position(puntoEntrega)
                .title("Panadería Lupita - Punto de entrega"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(puntoEntrega, 16f));
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }
}