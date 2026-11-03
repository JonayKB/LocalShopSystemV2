package es.jonay.kb.shopsystem.api.exceptions;

/** La imagen elegida ya la usa otro producto o categoría. */
public class ImageInUseException extends RuntimeException {
    public ImageInUseException(String message) {
        super(message);
    }
}
