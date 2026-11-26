package com.example.myfigmaprueba;

import org.junit.Test;
import static org.junit.Assert.*;

public class DineroTest {
    @Test public void conservaCentavosSinErrorDeComaFlotante() {
        assertEquals(10, Dinero.centavos("0.10"));
        assertEquals(2599, Dinero.centavos("25,99"));
        assertEquals(30, Math.multiplyExact(Dinero.centavos("0.10"), 3));
    }
    @Test public void admiteLimitesYNormalizaPreciosAnteriores() {
        assertEquals(99999999, Dinero.centavos("999999.99"));
        assertEquals(1, Dinero.centavos("0.01"));
        assertEquals(1235, Dinero.centavos(12.345));
    }
    @Test public void rechazaImportesInvalidos() {
        for (String texto : new String[]{"", "NaN", "Infinity", "-1", "0", "0.001", "1000000", "1e1000"}) {
            try { Dinero.centavos(texto); fail("Aceptó: " + texto); }
            catch (IllegalArgumentException esperado) { }
        }
    }
}
