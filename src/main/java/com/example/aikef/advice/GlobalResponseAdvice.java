package com.example.aikef.advice;

import com.example.aikef.dto.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.stream.Collectors;

/**
 * Global advice to standardize API responses and handle exceptions.
 * 1. Wraps successful responses in a Result object.
 * 2. Catches exceptions and formats them as a Result object.
 */
@RestControllerAdvice(basePackages = "com.example.aikef.controller")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final Logger logger = LoggerFactory.getLogger(GlobalResponseAdvice.class);

    private final ObjectMapper objectMapper;

    public GlobalResponseAdvice(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Do not wrap if the return type is already Result
        if (returnType.getParameterType().equals(Result.class)) {
            return false;
        }
        
        // Do not wrap if return type is Resource (file download/stream)
        if (Resource.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }
        
        // Do not wrap if return type is ResponseEntity<Resource>
        if (ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
            Type genericType = returnType.getGenericParameterType();
            if (genericType instanceof ParameterizedType parameterizedType) {
                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length > 0) {
                    Type typeArg = typeArguments[0];
                    if (typeArg instanceof Class<?> clazz && Resource.class.isAssignableFrom(clazz)) {
                        return false;
                    }
                    // Handle wildcards like ResponseEntity<? extends Resource>
                    if (typeArg instanceof java.lang.reflect.WildcardType wildcardType) {
                        Type[] upperBounds = wildcardType.getUpperBounds();
                        for (Type bound : upperBounds) {
                            if (bound instanceof Class<?> boundClass && Resource.class.isAssignableFrom(boundClass)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // For void methods, body is null
        if (body == null) {
            return Result.success();
        }

        // Do not wrap again if it's already a Result
        if (body instanceof Result) {
            return body;
        }

        // Do not wrap Resource types (file download/stream)
        if (body instanceof Resource) {
            return body;
        }

        // Special handling for String return types to avoid ClassCastException
        if (body instanceof String) {
            try {
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(Result.success(body));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize string response to JSON", e);
                return "{\"code\":500,\"message\":\"Internal Server Error\"}";
            }
        }

        return Result.success(body);
    }

    // --- Global Exception Handling ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        logger.warn("Validation failed: {}", errorMsg);
        return Result.error(HttpStatus.BAD_REQUEST.value(), errorMsg);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleEntityNotFound(EntityNotFoundException ex) {
        logger.warn("Entity not found: {}", ex.getMessage());
        return Result.error(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return Result.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler({AuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return Result.error(HttpStatus.UNAUTHORIZED.value(), "Authentication failed");
    }

    @ExceptionHandler({AccessDeniedException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return Result.error(HttpStatus.FORBIDDEN.value(), "Access denied");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleAllUncaughtException(Exception ex) {
        logger.error("An unexpected error occurred", ex);
        return Result.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An internal server error occurred");
    }
}
