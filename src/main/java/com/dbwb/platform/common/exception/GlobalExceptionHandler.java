package com.dbwb.platform.common.exception;

import com.dbwb.platform.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Translates domain exceptions into the shared ApiResponse envelope.
 * Per BR-NFR-010 / Section 14: messages returned to clients never leak
 * internal diagnostics (stack traces, entity internals) - only safe text.
 *
 * The handlers below the domain ones exist because everything without one fell
 * to the catch-all, and the catch-all says "an unexpected error occurred,
 * please contact support" with a 500. Measured against the running
 * application, that was the answer to a malformed body, a wrong type, an id
 * that is not a UUID, an unknown URL and a wrong HTTP method - five things the
 * caller got wrong, every one of them told the caller that we broke and to go
 * and find a human about it. Two costs, both real: the person cannot act on
 * it, and each one was logged at error with a stack trace, so the log that is
 * supposed to show a genuine crash was filling with routine mistakes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(BusinessRuleViolationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedForTenantException.class)
    public ResponseEntity<ApiResponse<Void>> handleTenantDenied(AccessDeniedForTenantException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Names the field that was wrong.
     *
     * This used to join the bare constraint messages, so a registration form
     * submitted with two fields empty answered "must not be blank; must not be
     * blank" - which does not say what was blank, and reads as a bug rather
     * than as something the person can fix.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> humanFieldName(error.getField()) + " " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    /** "fullName" -> "Full name", "whatsappNumber" -> "Whatsapp number". */
    private static String humanFieldName(String field) {
        String spaced = field.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('.', ' ').toLowerCase();
        return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /**
     * A body that could not be parsed at all, or a field of the wrong type -
     * "price": "free" where a number belongs. Deliberately vague about which
     * field: the parser's own message names Java classes and JSON paths, which
     * is diagnostic detail this must not return.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Rejected an unreadable request body", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Some of the information sent was not in the expected format. Please check the form and try again."));
    }

    /** An id in the URL that is not an id - a truncated link, or a typed one. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadPathValue(MethodArgumentTypeMismatchException ex) {
        log.debug("Rejected a badly typed request value", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("That link or address is not valid."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(humanFieldName(ex.getParameterName()) + " is required."));
    }

    /** An address nothing serves. A 404 in the shared envelope, not a 500. */
    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnknownAddress(Exception ex) {
        log.debug("No handler for the requested address", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("That address does not exist."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleWrongMethod(HttpRequestMethodNotSupportedException ex) {
        log.debug("Rejected an unsupported method", ex);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("That action is not available on this address."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleWrongContentType(HttpMediaTypeNotSupportedException ex) {
        log.debug("Rejected an unsupported content type", ex);
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error("That kind of content cannot be sent to this address."));
    }

    /**
     * Thrown by the multipart parser before any controller runs, so the upload
     * endpoint's own size check never sees it - without this it was a 500 for
     * the ordinary act of picking too large a photograph.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.debug("Rejected an oversized upload", ex);
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error("That file is too large to upload. Please choose a smaller one."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        // Intentionally generic to the client: internal details are logged server-side instead.
        log.error("Unhandled exception while processing request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again or contact support."));
    }
}
