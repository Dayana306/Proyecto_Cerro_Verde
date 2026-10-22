package pe.smcv.ptar.common.exception;

//http404
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
