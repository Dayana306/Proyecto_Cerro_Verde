package pe.smcv.ptar.convenio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * Cálculo del cumplimiento del convenio (lógica pura, probada de forma aislada).
 *
 * Para no sesgar el resultado cuando un día tiene más mediciones que otro, el promedio se calcula
 * como PROMEDIO DE PROMEDIOS DIARIOS: primero el promedio de cada día y luego el promedio de esos días.
 */
public final class CalculoCumplimiento {

    private static final int ESCALA = 3;

    private CalculoCumplimiento() {
    }

    public record Medicion(LocalDateTime fecha, BigDecimal valor) {}

    public record Mensual(int mes, BigDecimal promedio, int diasConDatos, int mediciones) {}

    public record Resultado(BigDecimal promedio, int diasConDatos, int mediciones, List<Mensual> meses) {}

    public static Resultado calcular(List<Medicion> mediciones) {
        // día -> [suma, cantidad]
        SortedMap<LocalDate, BigDecimal[]> porDia = new TreeMap<>();
        for (Medicion m : mediciones) {
            BigDecimal[] acc = porDia.computeIfAbsent(m.fecha().toLocalDate(),
                    d -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            acc[0] = acc[0].add(m.valor());
            acc[1] = acc[1].add(BigDecimal.ONE);
        }
        Map<LocalDate, BigDecimal> promedioDiario = new TreeMap<>();
        porDia.forEach((dia, acc) -> promedioDiario.put(dia, acc[0].divide(acc[1], 6, RoundingMode.HALF_UP)));

        // mes -> promedios diarios
        SortedMap<YearMonth, List<BigDecimal>> porMes = new TreeMap<>();
        SortedMap<YearMonth, Integer> medicionesPorMes = new TreeMap<>();
        promedioDiario.forEach((dia, prom) -> porMes.computeIfAbsent(YearMonth.from(dia), k -> new ArrayList<>()).add(prom));
        porDia.forEach((dia, acc) -> medicionesPorMes.merge(YearMonth.from(dia), acc[1].intValue(), Integer::sum));

        List<Mensual> meses = new ArrayList<>();
        porMes.forEach((ym, lista) -> meses.add(new Mensual(ym.getMonthValue(), promedio(lista), lista.size(),
                medicionesPorMes.getOrDefault(ym, 0))));

        return new Resultado(promedio(new ArrayList<>(promedioDiario.values())), promedioDiario.size(),
                mediciones.size(), meses);
    }

    /** % de la meta alcanzado (ej. 1.6 / 1.5 = 106.7). null si no hay datos. */
    public static BigDecimal porcentaje(BigDecimal promedio, BigDecimal meta) {
        if (promedio == null || meta == null || meta.signum() == 0) {
            return null;
        }
        return promedio.multiply(BigDecimal.valueOf(100)).divide(meta, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal promedio(List<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return null;
        }
        BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(valores.size()), ESCALA, RoundingMode.HALF_UP);
    }
}
