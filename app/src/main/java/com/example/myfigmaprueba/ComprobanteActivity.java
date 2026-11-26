package com.example.myfigmaprueba;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Exporta con el selector del sistema, sin permisos de almacenamiento. */
public abstract class ComprobanteActivity extends BaseActivity {
    private long pedidoExportando = -1;
    private final ActivityResultLauncher<String> documento = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/plain"), this::guardarComprobante);

    @Override protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        if (estado != null) pedidoExportando = estado.getLong("pedido_exportando", -1);
    }
    protected void descargar(Pedido pedido) {
        pedidoExportando = pedido.id;
        documento.launch("comprobante-" + pedido.folio() + ".txt");
    }
    private void guardarComprobante(Uri uri) {
        if (uri == null) return;
        try (DBHelper db = new DBHelper(this)) {
            Pedido pedido = db.obtenerPedido(pedidoExportando, sesion.getId());
            if (pedido == null) return;
            try (OutputStream salida = getContentResolver().openOutputStream(uri, "wt")) {
                if (salida == null) throw new IOException("Sin acceso al archivo");
                salida.write(pedido.comprobante().getBytes(StandardCharsets.UTF_8));
            }
            Toast.makeText(this, "Comprobante guardado", Toast.LENGTH_SHORT).show();
        } catch (IOException | RuntimeException e) {
            Toast.makeText(this, "No se pudo guardar el comprobante", Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putLong("pedido_exportando", pedidoExportando);
    }
}
