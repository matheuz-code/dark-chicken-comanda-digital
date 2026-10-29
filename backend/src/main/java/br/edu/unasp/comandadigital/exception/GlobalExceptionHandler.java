package br.edu.unasp.comandadigital.exception;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import br.edu.unasp.comandadigital.dto.error.ErroApiResponse;
import br.edu.unasp.comandadigital.dto.error.ErroCampoResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroApiResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ErroCampoResponse> campos = exception.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ErroCampoResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage() == null
                                ? "Valor inválido"
                                : fieldError.getDefaultMessage()))
                .distinct()
                .sorted(Comparator.comparing(ErroCampoResponse::campo)
                        .thenComparing(ErroCampoResponse::mensagem))
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "Existem campos inválidos na requisição",
                request,
                campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErroApiResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<ErroCampoResponse> campos = exception.getConstraintViolations().stream()
                .map(violation -> new ErroCampoResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .sorted(Comparator.comparing(ErroCampoResponse::campo))
                .toList();

        return response(
                HttpStatus.BAD_REQUEST,
                "Existem parâmetros inválidos na requisição",
                request,
                campos);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroApiResponse> handleMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "O corpo da requisição está ausente ou possui JSON inválido",
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErroApiResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "O parâmetro '" + exception.getName() + "' possui valor inválido",
                request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErroApiResponse> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErroApiResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "Acesso negado", request);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ErroApiResponse> handleResourceNotFound(
            RecursoNaoEncontradoException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroApiResponse> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Recurso não encontrado", request);
    }

    @ExceptionHandler(ConflitoException.class)
    ResponseEntity<ErroApiResponse> handleConflict(
            ConflitoException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroApiResponse> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        LOGGER.warn("Restrição de integridade violada em {}", request.getRequestURI());
        return response(
                HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade",
                request);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroApiResponse> handleBusinessRule(
            RegraNegocioException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroApiResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        LOGGER.error("Erro inesperado ao processar {}", request.getRequestURI(), exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado",
                request);
    }

    private ResponseEntity<ErroApiResponse> response(
            HttpStatus status,
            String mensagem,
            HttpServletRequest request) {
        return response(status, mensagem, request, List.of());
    }

    private ResponseEntity<ErroApiResponse> response(
            HttpStatus status,
            String mensagem,
            HttpServletRequest request,
            List<ErroCampoResponse> campos) {
        ErroApiResponse body = new ErroApiResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                campos);

        return ResponseEntity.status(status).body(body);
    }
}
