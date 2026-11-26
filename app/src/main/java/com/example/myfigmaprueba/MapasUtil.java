package com.example.myfigmaprueba;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public final class MapasUtil {
    private MapasUtil() { }
    public static boolean disponible(Context context) {
        try {
            Bundle datos = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            String clave = datos == null ? "" : datos.getString("com.google.android.geo.API_KEY", "");
            return !clave.trim().isEmpty() && GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
        } catch (PackageManager.NameNotFoundException e) { return false; }
    }
    public static void agregar(BaseActivity activity, LinearLayout panel, OnMapReadyCallback listo) {
        if (!disponible(activity)) {
            Pantalla.texto(panel, "Mapa no disponible. Puedes usar la dirección o las coordenadas.", false); return;
        }
        FrameLayout contenedor = new FrameLayout(activity); contenedor.setId(R.id.mapSeleccion);
        panel.addView(contenedor, new LinearLayout.LayoutParams(-1, Pantalla.dp(panel, 260)));
        SupportMapFragment mapa = (SupportMapFragment) activity.getSupportFragmentManager().findFragmentByTag("seleccion");
        if (mapa == null) {
            mapa = SupportMapFragment.newInstance();
            activity.getSupportFragmentManager().beginTransaction().replace(R.id.mapSeleccion, mapa, "seleccion").commitNow();
        }
        mapa.getMapAsync(listo);
    }
}
