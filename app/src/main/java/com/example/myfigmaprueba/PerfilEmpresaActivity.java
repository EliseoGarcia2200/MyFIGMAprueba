package com.example.myfigmaprueba;

import android.os.Bundle;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class PerfilEmpresaActivity extends BaseActivity {
    private EditText direccion, horario, telefono, descripcion, latitud, longitud;
    private final ActivityResultLauncher<Intent> ubicacion = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null && latitud != null) {
                    latitud.setText(String.valueOf(resultado.getData().getDoubleExtra("latitud", 0)));
                    longitud.setText(String.valueOf(resultado.getData().getDoubleExtra("longitud", 0)));
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!verificarAcceso()) return;
        LinearLayout panel = Pantalla.abrir(this, "Mi perfil");
        try (DBHelper db = new DBHelper(this)) {
            Usuario u = db.obtenerUsuario(sesion.getId());
            Pantalla.texto(panel, u.nombre, true);
            Pantalla.texto(panel, u.correo + "\nCuenta de " + (sesion.esPanaderia() ? "panadería" : "cliente"), false);

            // Botón visible arriba sin tener que hacer scroll
            Pantalla.boton(panel, "Cerrar sesión", v -> cerrarSesion());

            if (sesion.esPanaderia()) {
                Negocio n = db.obtenerNegocio(u.id);
                Pantalla.texto(panel, "Datos de recolección", true);
                Pantalla.texto(panel, "Guarda una dirección y un horario para habilitar las reservas. Las coordenadas son opcionales y permiten aparecer en el mapa.", false);
                direccion = Pantalla.campo(panel, "Dirección de recolección", n.direccion); direccion.setId(R.id.etNegocioDireccion);
                horario = Pantalla.campo(panel, "Días y horario de recolección", n.horario); horario.setId(R.id.etNegocioHorario);
                telefono = Pantalla.campo(panel, "Teléfono de contacto (opcional)", n.telefono, InputType.TYPE_CLASS_PHONE); telefono.setId(R.id.etNegocioTelefono);
                descripcion = Pantalla.campo(panel, "Descripción (opcional)", n.descripcion,
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); descripcion.setId(R.id.etNegocioDescripcion);
                int decimal = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
                latitud = Pantalla.campo(panel, "Latitud (opcional)", n.latitud == null ? "" : n.latitud.toString(), decimal); latitud.setId(R.id.etNegocioLatitud);
                longitud = Pantalla.campo(panel, "Longitud (opcional)", n.longitud == null ? "" : n.longitud.toString(), decimal); longitud.setId(R.id.etNegocioLongitud);
                Pantalla.boton(panel, "Elegir coordenadas en mapa", v -> {
                    Intent intent = new Intent(this, DireccionActivity.class).putExtra(DireccionActivity.EXTRA_SELECCION, true);
                    try {
                        Double lat = coordenada(latitud), lon = coordenada(longitud);
                        if (lat != null && lon != null) { intent.putExtra("latitud", lat.doubleValue()); intent.putExtra("longitud", lon.doubleValue()); }
                    }
                    catch (NumberFormatException ignored) { }
                    ubicacion.launch(intent);
                });
                Pantalla.boton(panel, "Guardar datos de la panadería", v -> guardar());
                Pantalla.boton(panel, "Mis publicaciones", v -> startActivity(new Intent(this, MisPublicacionesActivity.class)));
                Pantalla.boton(panel, "Crear publicación", v -> startActivity(new Intent(this, EditarPublicacionActivity.class)));
            }
            Pantalla.boton(panel, sesion.esPanaderia() ? "Mis pedidos" : "Mis compras", v -> startActivity(new Intent(this,
                    sesion.esPanaderia() ? MisPedidosActivity.class : MisComprasActivity.class)));
        }
    }

    private Double coordenada(EditText campo) {
        String texto = campo.getText().toString().trim();
        return texto.isEmpty() ? null : Double.valueOf(texto.replace(',', '.'));
    }

    private void guardar() {
        if (direccion.getText().toString().trim().isEmpty()) { direccion.setError("Escribe la dirección"); return; }
        if (horario.getText().toString().trim().isEmpty()) { horario.setError("Escribe los días y horarios"); return; }
        try (DBHelper db = new DBHelper(this)) {
            Double lat = coordenada(latitud), lon = coordenada(longitud);
            if (!db.guardarNegocio(sesion.getId(), direccion.getText().toString(), horario.getText().toString(),
                    telefono.getText().toString(), descripcion.getText().toString(), lat, lon)) {
                latitud.setError("Introduce ambas coordenadas válidas o deja ambas vacías"); return;
            }
            Toast.makeText(this, "Datos de recolección guardados", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) { latitud.setError("Coordenadas no válidas"); }
    }
}