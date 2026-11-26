package com.example.myfigmaprueba;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.InputStream;
import java.util.Collections;

public final class QrUtil {
    public static final String PREFIJO = "cerodesperdicio:pedido:";
    private QrUtil() { }
    public static Bitmap generar(String token) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(PREFIJO + token, BarcodeFormat.QR_CODE, 640, 640);
        int[] pixeles = new int[640 * 640];
        for (int y = 0; y < 640; y++) for (int x = 0; x < 640; x++)
            pixeles[y * 640 + x] = matrix.get(x, y) ? 0xff000000 : 0xffffffff;
        return Bitmap.createBitmap(pixeles, 640, 640, Bitmap.Config.ARGB_8888);
    }

    public static String leer(Context context, Uri uri) throws Exception {
        BitmapFactory.Options opciones = new BitmapFactory.Options(); opciones.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, opciones); }
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) throw new IllegalArgumentException("Imagen no válida");
        opciones.inSampleSize = 1;
        while ((long) (opciones.outWidth / opciones.inSampleSize) * (opciones.outHeight / opciones.inSampleSize) > 4000000)
            opciones.inSampleSize *= 2;
        opciones.inJustDecodeBounds = false;
        Bitmap foto;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) { foto = BitmapFactory.decodeStream(in, null, opciones); }
        if (foto == null) throw new IllegalArgumentException("Imagen no válida");
        try {
            int w = foto.getWidth(), h = foto.getHeight(); int[] pixels = new int[w * h];
            foto.getPixels(pixels, 0, w, 0, 0, w, h);
            return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w, h, pixels))),
                    Collections.singletonMap(DecodeHintType.TRY_HARDER, true)).getText();
        } finally { foto.recycle(); }
    }
}
