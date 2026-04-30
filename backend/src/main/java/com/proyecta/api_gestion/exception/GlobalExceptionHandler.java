package com.proyecta.api_gestion.exception;

import com.proyecta.api_gestion.dto.common.ApiErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para toda la API.
 * Centraliza el manejo de errores y garantiza respuestas consistentes
 * usando ApiErrorResponseDTO para todos los códigos 4xx y 5xx.
 *
 * Jerarquía de manejo:
 * - 400: Validación de datos, parámetros inválidos, sintaxis incorrecta
 * - 401: Errores de autenticación (token ausente, credenciales inválidas)
 * - 403: Errores de autorización (permisos insuficientes)
 * - 404: Recursos no encontrados
 * - 405: Métodos HTTP no permitidos
 * - 415: Tipos de contenido no soportados
 * - 500: Errores internos no controlados
 * - 503: Servicios externos no disponibles (BD, IO)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 4xx - Errores del Cliente ====================

    /**
     * Maneja excepciones de validación de Bean Validation (@Valid).
     * Retorna detalles de cada campo inválido.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponseDTO(422, "Unprocessable Entity",
                        "Validación fallida: " + details, request.getRequestURI()));
    }

    /**
     * Maneja errores de sintaxis JSON o cuerpos mal formados.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponseDTO(400, "Bad Request",
                        "Cuerpo de la solicitud mal formado: " + ex.getMostSpecificCause().getMessage(),
                        request.getRequestURI()));
    }

    /**
     * Maneja errores de BadRequestException personalizada.
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleBadRequest(
            BadRequestException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponseDTO(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Maneja errores de IllegalArgumentException.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponseDTO(400, "Bad Request", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Maneja métodos HTTP no permitidos en el endpoint.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ApiErrorResponseDTO(405, "Method Not Allowed",
                        "Método '" + ex.getMethod() + "' no permitido. Métodos soportados: " +
                                String.join(", ", ex.getSupportedMethods()), request.getRequestURI()));
    }

    /**
     * Maneja tipos de contenido no soportados.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiErrorResponseDTO(415, "Unsupported Media Type",
                        "Tipo de contenido '" + ex.getContentType() + "' no soportado", request.getRequestURI()));
    }

    // ==================== 401 - Errores de Autenticación ====================

    /**
     * Maneja errores de credenciales inválidas (Spring Security).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponseDTO(401, "Unauthorized",
                        "Credenciales de autenticación inválidas", request.getRequestURI()));
    }

    /**
     * Maneja UnauthorizedException personalizada.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleUnauthorized(
            UnauthorizedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponseDTO(401, "Unauthorized", ex.getMessage(), request.getRequestURI()));
    }

    // ==================== 403 - Errores de Autorización ====================

    /**
     * Maneja errores de acceso denegado (Spring Security).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponseDTO(403, "Forbidden",
                        "No tiene permisos para acceder a este recurso", request.getRequestURI()));
    }

    /**
     * Maneja ForbiddenException personalizada.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponseDTO(403, "Forbidden", ex.getMessage(), request.getRequestURI()));
    }

    // ==================== 404 - Recursos No Encontrados ====================

    /**
     * Maneja ResourceNotFoundException personalizada.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponseDTO(404, "Not Found", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Maneja rutas estáticas no encontradas.
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiErrorResponseDTO> handleResourceNotFound(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponseDTO(404, "Not Found",
                        "El recurso solicitado no fue encontrado: " + request.getRequestURI(),
                        request.getRequestURI()));
    }

    // ==================== 503 - Servicio No Disponible ====================

    /**
     * Maneja el error "Could not open JPA EntityManager for transaction".
     * Ocurre cuando la base de datos no está disponible o el pool de conexiones
     * está agotado. Retorna 503 en lugar de 500.
     */
    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleCannotCreateTransaction(
            CannotCreateTransactionException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponseDTO(503, "Service Unavailable",
                        "Servicio no disponible", request.getRequestURI()));
    }

    /**
     * Maneja errores internos de JPA (conexión perdida, queries erróneas, etc.).
     */
    @ExceptionHandler(JpaSystemException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleJpaSystemException(
            JpaSystemException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponseDTO(503, "Service Unavailable",
                        "Servicio no disponible", request.getRequestURI()));
    }

    /**
     * Maneja excepciones de acceso a base de datos (consultas fallidas, violaciones de restricción).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleDataAccessError(
            DataAccessException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponseDTO(503, "Service Unavailable",
                        "Servicio no disponible", request.getRequestURI()));
    }

    /**
     * Maneja ServiceUnavailableException personalizada.
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleServiceUnavailable(
            ServiceUnavailableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponseDTO(503, "Service Unavailable",
                        "Servicio no disponible", request.getRequestURI()));
    }

    /**
     * Maneja errores de I/O que pueden indicar problemas de infraestructura.
     */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleIOException(
            java.io.IOException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiErrorResponseDTO(503, "Service Unavailable",
                        "Error de entrada/salida: " + ex.getMessage(), request.getRequestURI()));
    }

    // ==================== 500 - Error Interno del Servidor ====================

    /**
     * Maneja errores de serialización JSON (ej. referencias circulares, StackOverflowError).
     * Garantiza que el cliente reciba un JSON de error limpio en lugar de una respuesta truncada.
     */
    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleHttpMessageNotWritable(
            HttpMessageNotWritableException ex, HttpServletRequest request) {
        String cause = ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "desconocido";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponseDTO(500, "Internal Server Error",
                        "Error al serializar la respuesta (" + cause + "). Revise las relaciones circulares en los modelos.",
                        request.getRequestURI()));
    }

    /**
     * Maneja InternalErrorException personalizada (errores de sistema de archivos, storage).
     */
    @ExceptionHandler(com.proyecta.api_gestion.exception.InternalErrorException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleInternalErrorException(
            com.proyecta.api_gestion.exception.InternalErrorException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponseDTO(500, "Internal Server Error",
                        ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Maneja errores de validación de Bean Validation en parámetros de ruta y consulta.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleConstraintViolationException(
            jakarta.validation.ConstraintViolationException ex, HttpServletRequest request) {
        String details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponseDTO(400, "Bad Request",
                        "Validación de parámetros fallida: " + details, request.getRequestURI()));
    }

    /**
     * Maneja UnprocessableEntityException personalizada.
     */
    @ExceptionHandler(UnprocessableEntityException.class)
    public ResponseEntity<ApiErrorResponseDTO> handleUnprocessableEntity(
            UnprocessableEntityException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponseDTO(422, "Unprocessable Entity", ex.getMessage(), request.getRequestURI()));
    }

    /**
     * Maneja cualquier excepción no controlada como error interno.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDTO> handleInternalError(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponseDTO(500, "Internal Server Error",
                        "Ha ocurrido un error inesperado al procesar la solicitud: " + ex.getMessage(), request.getRequestURI()));
    }
}
