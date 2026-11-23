package com.example.myfigmaprueba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Todo lo de fotos: copiarlas a la app (reducidas y derechas), cargarlas y borrarlas. */
public final class ImagenUtil {

    private static final int LADO_MAX = 1024;   // la foto guardada no pasa de 1024 px
    private static final int CALIDAD_JPEG = 85;

    private ImagenUtil() { }

    /**
     * Copia la foto elegida a la carpeta privada de la app, reducida y con la orientación
     * corregida. Devuelve la ruta del archivo, o null si algo falló.
     */
    public static String guardarDesdeUri(Context context, Uri uri) {
        try {
            // 1) Medir la foto sin cargarla completa en memoria.
            BitmapFactory.Options medidas = new BitmapFactory.Options();
            medidas.inJustDecodeBounds = true;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                BitmapFactory.decodeStream(in, null, medidas);
            }
            if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null;

            // 2) Cargarla ya reducida.
            BitmapFactory.Options opciones = new BitmapFactory.Options();
            opciones.inSampleSize = calcularMuestreo(medidas.outWidth, medidas.outHeight, LADO_MAX);
            Bitmap bitmap;
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) return null;
                bitmap = BitmapFactory.decodeStream(in, null, opciones);
            }
            if (bitmap == null) return null;

            // 3) Enderezarla (muchas fotos de celular vienen "acostadas" y traen la orientación aparte).
            int grados = leerRotacion(context, uri);
            if (grados != 0) {
                Matrix matriz = new Matrix();
                matriz.postRotate(grados);
                Bitmap rotada = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), matriz, true);
                if (rotada != bitmap) bitmap.recycle();
                bitmap = rotada;
            }

            // 4) Ajustar a 1024 px como máximo.
            int mayor = Math.max(bitmap.getWidth(), bitmap.getHeight());
            if (mayor > LADO_MAX) {
                float escala = (float) LADO_MAX / mayor;
                Bitmap escalada = Bitmap.createScaledBitmap(bitmap,
                        Math.round(bitmap.getWidth() * escala),
                        Math.round(bitmap.getHeight() * escala), true);
                if (escalada != bitmap) bitmap.recycle();
                bitmap = escalada;
            }

            // 5) Guardar como JPEG en la carpeta privada de la app.
            File carpeta = new File(context.getFilesDir(), "publicaciones");
            if (!carpeta.exists() && !carpeta.mkdirs()) return null;
            File archivo = new File(carpeta, "pub_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream salida = new FileOutputStream(archivo)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida);
            }
            bitmap.recycle();
            return archivo.getAbsolutePath();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Carga una foto guardada, reducida a un tamaño cercano a ladoObjetivo (para listas y vistas). */
    public static Bitmap cargar(String ruta, int ladoObjetivo) {
        if (ruta == null) return null;
        BitmapFactory.Options medidas = new BitmapFactory.Options();
        medidas.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(ruta, medidas);
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) return null;

        BitmapFactory.Options opciones = new BitmapFactory.Options();
        opciones.inSampleSize = calcularMuestreo(medidas.outWidth, medidas.outHeight, ladoObjetivo);
        return BitmapFactory.decodeFile(ruta, opciones);
    }

    /** Borra el archivo de una foto (si existe). */
    public static void borrar(String ruta) {
        if (ruta != null) {
            //noinspection ResultOfMethodCallIgnored
            new File(ruta).delete();
        }
    }

    /** Cuánto reducir al decodificar sin quedar por debajo de ladoMax en ningún lado. */
    private static int calcularMuestreo(int ancho, int alto, int ladoMax) {
        int muestreo = 1;
        while (ancho / (muestreo * 2) >= ladoMax && alto / (muestreo * 2) >= ladoMax) {
            muestreo *= 2;
        }
        return muestreo;
    }

    private static int leerRotacion(Context context, Uri uri) {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return 0;
            ExifInterface exif = new ExifInterface(in);
            int orientacion = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientacion) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default: return 0;
            }
        } catch (IOException e) {
            return 0;
        }
    }
}
