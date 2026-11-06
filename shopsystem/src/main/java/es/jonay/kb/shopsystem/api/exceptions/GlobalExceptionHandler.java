package es.jonay.kb.shopsystem.api.exceptions;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Errores coherentes para toda la API. El cuerpo es el mensaje en texto plano (el panel lo
 * muestra tal cual). Los errores inesperados no filtran detalles internos: se registran en
 * el log con un código que también recibe el cliente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = Logger.getLogger(GlobalExceptionHandler.class.getName());

    private static ResponseEntity<String> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8))
                .body(message);
    }

    @ExceptionHandler({ IllegalArgumentException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class })
    public ResponseEntity<String> badRequest(Exception e) {
        String msg = e instanceof MethodArgumentTypeMismatchException m
                ? "Valor no válido para " + m.getName()
                : e.getMessage();
        return body(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> unreadable(HttpMessageNotReadableException e) {
        return body(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos");
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> notFound(NoSuchElementException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<String> noResource(NoResourceFoundException e) {
        return body(HttpStatus.NOT_FOUND, "No encontrado");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> method(HttpRequestMethodNotSupportedException e) {
        return body(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido");
    }

    @ExceptionHandler({ ConflictException.class, ImageInUseException.class })
    public ResponseEntity<String> conflict(RuntimeException e) {
        return body(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> integrity(DataIntegrityViolationException e) {
        logger.log(Level.WARNING, "Violación de integridad", e);
        return body(HttpStatus.CONFLICT, "La operación choca con datos existentes (código o nombre repetido, o datos relacionados)");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<String> tooLarge(MaxUploadSizeExceededException e) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "El archivo es demasiado grande");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> unauthorized(InvalidCredentialsException e) {
        return body(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.ErrorResponseException.class)
    public ResponseEntity<String> errorResponse(org.springframework.web.ErrorResponseException e) {
        String detail = e.getBody().getDetail();
        return ResponseEntity.status(e.getStatusCode())
                .contentType(new MediaType(MediaType.TEXT_PLAIN, java.nio.charset.StandardCharsets.UTF_8))
                .body(detail != null ? detail : e.getStatusCode().toString());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> unexpected(Exception e) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        logger.log(Level.SEVERE, "Error inesperado [" + id + "]", e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor (código " + id + ")");
    }
}
