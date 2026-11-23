package com.example.myfigmaprueba;

import android.graphics.Bitmap;
import android.util.LruCache;
import android.widget.ImageView;

/** Muestra fotos pequeñas en las listas, guardando las ya cargadas para que el scroll sea fluido. */
public final class Miniaturas {

    /** Caché compartida de toda la app (máx. 6 MB). */
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(6 * 1024 * 1024) {
                @Override
                protected int sizeOf(String clave, Bitmap bitmap) {
                    return bitmap.getByteCount();
                }
            };

    private Miniaturas() { }

    /** Pone la foto en la vista; si no hay foto, deja el ícono gris por defecto. */
    public static void mostrar(ImageView vista, String ruta, int lado) {
        Bitmap bitmap = obtener(ruta, lado);
        if (bitmap != null) {
            vista.setScaleType(ImageView.ScaleType.CENTER_CROP);
            vista.setImageBitmap(bitmap);
        } else {
            // Las tarjetas se reutilizan: sin foto hay que volver al ícono por defecto.
            vista.setScaleType(ImageView.ScaleType.FIT_CENTER);
            vista.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private static Bitmap obtener(String ruta, int lado) {
        if (ruta == null) return null;
        String clave = ruta + "@" + lado;
        Bitmap bitmap = CACHE.get(clave);
        if (bitmap == null) {
            bitmap = ImagenUtil.cargar(ruta, lado);
            if (bitmap != null) CACHE.put(clave, bitmap);
        }
        return bitmap;
    }
}
