package io.pesira.records.common;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

// Extends Spring's handler so framework errors (bad JSON, type mismatch, 404 route, 405, ...)
// keep their own status. The catch-all below only sees exceptions nothing else matches.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record InvalidField(String field, String message) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
                .toList();
        return handleExceptionInternal(ex, validationProblem(fields), headers, status, request);
    }

    // Invalid @RequestParam values such as size=101.
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<InvalidField> fields = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new InvalidField(result.getMethodParameter().getParameterName(),
                                error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(ex, validationProblem(fields), headers, status, request);
    }

    @ExceptionHandler
    ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler
    ProblemDetail handleConflict(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    // Safety net for races the service checks miss, e.g. the unique email index.
    @ExceptionHandler
    ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The request conflicts with existing data");
    }

    @ExceptionHandler
    ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The record was changed by someone else. Reload it and try again");
    }

    // Thrown by @PreAuthorize; without this the catch-all would turn it into a 500.
    @ExceptionHandler
    ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "You are not allowed to do this");
    }

    @ExceptionHandler
    ProblemDetail handleAuthentication(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Authentication is required");
    }

    @ExceptionHandler
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private static ProblemDetail validationProblem(List<InvalidField> fields) {
        String detail = fields.size() == 1 ? "1 field is invalid" : fields.size() + " fields are invalid";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setProperty("fieldErrors", fields);
        return problem;
    }
}
