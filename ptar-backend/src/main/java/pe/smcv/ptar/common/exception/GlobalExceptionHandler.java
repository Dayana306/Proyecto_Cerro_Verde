package pe.smcv.ptar.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.smcv.ptar.common.dto.ApiError;

import java.util.LinkedHashMap;
import java.util.Map;

/** Convierte cualquier excepción en el formato único ApiError. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req, ex.getErrores());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errores.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errores.putIfAbsent(ge.getObjectName(), ge.getDefaultMessage()));
        return build(HttpStatus.BAD_REQUEST, "Datos de entrada inválidos", req, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST,
                "El cuerpo de la solicitud es inválido (JSON mal formado o valor no permitido)", req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> tipoInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro '" + ex.getName() + "'", req, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> archivoGrande(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo supera el tamaño máximo permitido", req, null);
    }

    /** Login fallido. Mensaje genérico para no revelar si el correo existe. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> autenticacion(AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciales inválidas o cuenta inactiva", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "No tiene permisos para realizar esta operación", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violación de integridad: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "La operación viola una restricción de la base de datos (dato duplicado o referencia inválida)", req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> inesperado(Exception ex, HttpServletRequest req) {
        // Excepciones propias de Spring MVC (404 de ruta, 405, parámetro faltante...) traen su propio estado.
        if (ex instanceof ErrorResponse er) {
            HttpStatusCode code = er.getStatusCode();
            HttpStatus status = HttpStatus.resolve(code.value());
            String mensaje = er.getBody().getDetail() != null ? er.getBody().getDetail() : "Solicitud inválida";
            return build(status != null ? status : HttpStatus.BAD_REQUEST, mensaje, req, null);
        }
        log.error("Error inesperado en {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", req, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest req,
                                           Map<String, String> errores) {
        ApiError body = ApiError.of(status.value(), status.getReasonPhrase(), message,
                req.getRequestURI(), errores);
        return ResponseEntity.status(status).body(body);
    }
}
