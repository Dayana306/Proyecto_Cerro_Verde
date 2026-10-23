package pe.smcv.ptar.common.exception;

import java.util.Map;

/** Violación de una regla de negocio, p. ej. correo duplicado o valor fuera de rango (HTTP 422). */
public class ReglaNegocioException extends RuntimeException {

    private final transient Map<String, String> errores;

    public ReglaNegocioException(String message) {
        this(message, null);
    }

    /** errores: detalle por campo (ej. "lecturas[0].valor" -> "fuera del rango válido"). */
    public ReglaNegocioException(String message, Map<String, String> errores) {
        super(message);
        this.errores = errores;
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
