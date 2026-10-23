package pe.smcv.ptar.parametro.service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Regla de alertas (RF-05). Lógica pura, sin Spring ni BD, para poder probarla de forma aislada.
 *
 * <pre>
 *   valor fuera de [criticoMin, criticoMax]  -> ALTA
 *   valor fuera de [limiteMin,  limiteMax ]  -> MEDIA
 *   en otro caso                             -> sin alerta
 * </pre>
 * Cualquier límite puede ser null (no se evalúa ese lado). El valor exactamente igual al límite NO genera alerta.
 */
public final class EvaluadorUmbral {

    public enum Nivel { MEDIA, ALTA }

    public enum Tipo { MAXIMO_EXCEDIDO, MINIMO_EXCEDIDO }

    public record Resultado(Nivel nivel, Tipo tipo) {}

    private EvaluadorUmbral() {
    }

    public static Optional<Resultado> evaluar(BigDecimal valor,
                                              BigDecimal limiteMin, BigDecimal limiteMax,
                                              BigDecimal criticoMin, BigDecimal criticoMax) {
        if (valor == null) {
            return Optional.empty();
        }
        if (mayor(valor, criticoMax)) {
            return Optional.of(new Resultado(Nivel.ALTA, Tipo.MAXIMO_EXCEDIDO));
        }
        if (menor(valor, criticoMin)) {
            return Optional.of(new Resultado(Nivel.ALTA, Tipo.MINIMO_EXCEDIDO));
        }
        if (mayor(valor, limiteMax)) {
            return Optional.of(new Resultado(Nivel.MEDIA, Tipo.MAXIMO_EXCEDIDO));
        }
        if (menor(valor, limiteMin)) {
            return Optional.of(new Resultado(Nivel.MEDIA, Tipo.MINIMO_EXCEDIDO));
        }
        return Optional.empty();
    }

    /**
     * Valida la coherencia de un umbral. Devuelve el mensaje de error o null si es válido.
     * Reglas: al menos un límite; min <= max en cada par; el rango crítico debe contener al de alerta.
     */
    public static String validar(BigDecimal limiteMin, BigDecimal limiteMax,
                                 BigDecimal criticoMin, BigDecimal criticoMax) {
        if (limiteMin == null && limiteMax == null && criticoMin == null && criticoMax == null) {
            return "Debe indicar al menos un límite";
        }
        if (limiteMin != null && limiteMax != null && limiteMin.compareTo(limiteMax) > 0) {
            return "limiteMin no puede ser mayor que limiteMax";
        }
        if (criticoMin != null && criticoMax != null && criticoMin.compareTo(criticoMax) > 0) {
            return "criticoMin no puede ser mayor que criticoMax";
        }
        if (criticoMin != null && limiteMin != null && criticoMin.compareTo(limiteMin) > 0) {
            return "criticoMin debe ser menor o igual que limiteMin (el nivel crítico es más extremo)";
        }
        if (criticoMax != null && limiteMax != null && criticoMax.compareTo(limiteMax) < 0) {
            return "criticoMax debe ser mayor o igual que limiteMax (el nivel crítico es más extremo)";
        }
        return null;
    }

    private static boolean mayor(BigDecimal valor, BigDecimal limite) {
        return limite != null && valor.compareTo(limite) > 0;
    }

    private static boolean menor(BigDecimal valor, BigDecimal limite) {
        return limite != null && valor.compareTo(limite) < 0;
    }
}
