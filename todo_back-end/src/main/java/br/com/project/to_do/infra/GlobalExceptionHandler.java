package br.com.project.to_do.infra;

import br.com.project.to_do.dto.ApiErrorResponseDTO;
import br.com.project.to_do.dto.FieldErrorResponseDTO;
import br.com.project.to_do.exception.BusinessRuleException;
import br.com.project.to_do.exception.InvalidTokenException;
import br.com.project.to_do.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorResponseDTO> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapFieldError)
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Dados inválidos.", request, fieldErrors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler({BusinessRuleException.class, BadCredentialsException.class, InvalidTokenException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleBusinessErrors(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = exception instanceof InvalidTokenException || exception instanceof BadCredentialsException
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_REQUEST;
        String message = exception instanceof BadCredentialsException
                ? "Credenciais inválidas."
                : exception.getMessage();
        return buildResponse(status, message, request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDTO> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro inesperado ao processar a requisição.",
                request,
                List.of()
        );
    }

    private ResponseEntity<ApiErrorResponseDTO> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<FieldErrorResponseDTO> fieldErrors
    ) {
        return ResponseEntity.status(status).body(new ApiErrorResponseDTO(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                fieldErrors
        ));
    }

    private FieldErrorResponseDTO mapFieldError(FieldError fieldError) {
        return new FieldErrorResponseDTO(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
