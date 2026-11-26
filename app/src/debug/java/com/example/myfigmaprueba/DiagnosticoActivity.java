package com.example.myfigmaprueba;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

/** Pantalla nativa en un proceso separado para que el cierre no oculte su causa. */
public class DiagnosticoActivity extends Activity {
    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        File archivo = new File(getFilesDir(), AplicacionDiagnostico.ARCHIVO);
        String informe;
        try (FileInputStream entrada = new FileInputStream(archivo)) {
            byte[] datos = new byte[16000]; int leidos = entrada.read(datos);
            informe = leidos > 0 ? new String(datos, 0, leidos, StandardCharsets.UTF_8) : "Informe vacío";
        } catch (Exception e) { informe = "No se pudo leer el diagnóstico: " + e.getClass().getSimpleName(); }
        final String textoInforme = informe;
        LinearLayout contenido = new LinearLayout(this); contenido.setOrientation(LinearLayout.VERTICAL);
        int margen = Math.round(20 * getResources().getDisplayMetrics().density);
        contenido.setPadding(margen, margen, margen, margen);
        TextView titulo = new TextView(this); titulo.setText("No se pudo iniciar la aplicación"); titulo.setTextSize(22);
        contenido.addView(titulo);
        TextView explicacion = new TextView(this);
        explicacion.setText("Copia este diagnóstico y pégalo en el chat para localizar el cierre. Tus cuentas y publicaciones se conservan.");
        explicacion.setTextSize(16); explicacion.setPadding(0, margen, 0, margen); contenido.addView(explicacion);
        Button copiar = new Button(this); copiar.setText("Copiar diagnóstico"); contenido.addView(copiar);
        copiar.setOnClickListener(v -> {
            ClipboardManager portapapeles = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            portapapeles.setPrimaryClip(ClipData.newPlainText("Diagnóstico de Cero Desperdicio", textoInforme));
            Toast.makeText(this, "Diagnóstico copiado", Toast.LENGTH_SHORT).show();
        });
        Button reintentar = new Button(this); reintentar.setText("Volver a intentar"); contenido.addView(reintentar);
        reintentar.setOnClickListener(v -> {
            if (archivo.exists() && !archivo.delete()) {
                Toast.makeText(this, "No se pudo cerrar el informe", Toast.LENGTH_SHORT).show(); return;
            }
            startActivity(new Intent(this, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish();
        });
        ScrollView desplazamiento = new ScrollView(this);
        TextView detalle = new TextView(this); detalle.setText(textoInforme); detalle.setTextSize(14);
        detalle.setTextIsSelectable(true); detalle.setPadding(0, margen, 0, margen); desplazamiento.addView(detalle);
        contenido.addView(desplazamiento, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(contenido);
    }
}
