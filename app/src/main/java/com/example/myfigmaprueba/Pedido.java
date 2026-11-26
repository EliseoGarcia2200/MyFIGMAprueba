package com.example.myfigmaprueba;

import android.database.Cursor;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Copia inmutable de lo acordado: editar o borrar una publicación no altera una compra. */
public final class Pedido {
    public static final String EXTRA_ID = "pedido_id";
    public static final String RESERVADO = "reservado", ENTREGADO = "entregado",
            CANCELADO = "cancelado", VENCIDO = "vencido";
    public final long id, clienteId, panaderiaId, publicacionId, precioCentavos, creado, vence;
    public final int cantidad;
    public final String producto, detalles, imagen, cliente, correoCliente, panaderia,
            direccion, horario, estado, token;
    public final Double latitud, longitud;

    Pedido(Cursor c) {
        id = numero(c, "id"); clienteId = numero(c, "cliente_id");
        panaderiaId = numero(c, "panaderia_id"); publicacionId = numero(c, "publicacion_id");
        precioCentavos = numero(c, "precio_centavos"); cantidad = (int) numero(c, "cantidad");
        creado = numero(c, "creado"); vence = numero(c, "vence");
        producto = texto(c, "producto"); detalles = texto(c, "detalles"); imagen = texto(c, "imagen");
        cliente = texto(c, "cliente"); correoCliente = texto(c, "correo_cliente");
        panaderia = texto(c, "panaderia"); direccion = texto(c, "direccion");
        horario = texto(c, "horario"); estado = texto(c, "estado"); token = texto(c, "token");
        int lat = c.getColumnIndexOrThrow("latitud"), lon = c.getColumnIndexOrThrow("longitud");
        latitud = c.isNull(lat) ? null : c.getDouble(lat);
        longitud = c.isNull(lon) ? null : c.getDouble(lon);
    }

    private static long numero(Cursor c, String campo) { return c.getLong(c.getColumnIndexOrThrow(campo)); }
    private static String texto(Cursor c, String campo) { return c.getString(c.getColumnIndexOrThrow(campo)); }
    public long totalCentavos() { return Math.multiplyExact(precioCentavos, cantidad); }
    public String folio() { return String.format(Locale.ROOT, "CD-%06d", id); }
    public boolean activo() { return RESERVADO.equals(estado) && vence > System.currentTimeMillis(); }
    public String estadoTexto() {
        switch (estado) {
            case ENTREGADO: return "Entregado y cobrado";
            case CANCELADO: return "Cancelado";
            case VENCIDO: return "Reserva vencida";
            default: return "Reservado · pago al recoger";
        }
    }
    public static String fecha(long instante) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(instante));
    }
    public String comprobante() {
        return "CERO DESPERDICIO\nComprobante de reserva " + folio() + "\n\n"
                + "Estado: " + estadoTexto() + "\nFecha: " + fecha(creado)
                + "\nCliente: " + cliente + "\nPanadería: " + panaderia
                + "\nProducto: " + producto + "\nCantidad: " + cantidad
                + "\nPrecio unitario: " + Dinero.formato(precioCentavos)
                + "\nTotal: " + Dinero.formato(totalCentavos())
                + "\nPago: efectivo al recoger" + (ENTREGADO.equals(estado) ? " (cobrado)" : " (sin cobro registrado)")
                + "\nDirección: " + direccion + "\nHorario informado: " + horario
                + "\nLímite de reserva: " + fecha(vence)
                + "\n\nEste comprobante no es una factura fiscal.\n";
    }
}
