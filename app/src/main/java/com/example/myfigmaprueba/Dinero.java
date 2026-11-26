package com.example.myfigmaprueba;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Los pedidos conservan importes exactos en centavos, nunca totales en double. */
public final class Dinero {
    private Dinero() { }

    public static long centavos(String texto) {
        try {
            BigDecimal valor = new BigDecimal(texto.trim().replace(',', '.'));
            long centavos = valor.setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2).longValueExact();
            if (centavos < 1 || centavos > 99999999) throw new ArithmeticException();
            return centavos;
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Precio entre $0.01 y $999,999.99, con máximo dos decimales");
        }
    }

    public static long centavos(double precio) {
        return centavos(BigDecimal.valueOf(precio).setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    public static String formato(long centavos) {
        return NumberFormat.getCurrencyInstance(new Locale("es", "MX"))
                .format(BigDecimal.valueOf(centavos, 2)) + " MXN";
    }
}
