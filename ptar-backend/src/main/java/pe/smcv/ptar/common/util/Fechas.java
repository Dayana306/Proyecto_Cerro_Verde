package pe.smcv.ptar.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class Fechas {

    private Fechas() {
    }

    /** Inicio del día (00:00) o null. */
    public static LocalDateTime inicio(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay();
    }

    /** Fin del día (23:59:59.999999) o null. */
    public static LocalDateTime fin(LocalDate fecha) {
        return fecha == null ? null : fecha.atTime(LocalTime.MAX);
    }
}
