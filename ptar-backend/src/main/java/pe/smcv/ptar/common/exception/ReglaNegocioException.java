package pe.smcv.ptar.common.exception;

//violacion de una regla
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String message) {
        super(message);
    }
}
