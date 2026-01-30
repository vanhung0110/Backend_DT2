package com.example.hungdt2.common;

import com.example.hungdt2.exceptions.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException ex) {
        ApiError error = new ApiError(ex.getCode(), ex.getMessage(), null);
        return new ResponseEntity<>(Map.of("error", error), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException ex) {
        ApiError error = new ApiError(ex.getCode(), ex.getMessage(), null);
        return new ResponseEntity<>(Map.of("error", error), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
        ApiError error = new ApiError(ex.getCode(), ex.getMessage(), null);
        return new ResponseEntity<>(Map.of("error", error), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenException ex) {
        ApiError error = new ApiError(ex.getCode(), ex.getMessage(), null);
        return new ResponseEntity<>(Map.of("error", error), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxSize(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        ApiError error = new ApiError("UPLOAD_TOO_LARGE", "File size exceeds limit", null);
        return new ResponseEntity<>(Map.of("error", error), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.put(fe.getField(), fe.getDefaultMessage());
        }
        ApiError error = new ApiError("VALIDATION_ERROR", "Validation failed", details);
        return new ResponseEntity<>(Map.of("error", error), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex) {
        log.error("Unhandled exception: " + ex.getMessage(), ex);
        ApiError error = new ApiError("INTERNAL_ERROR", "Internal server error: " + ex.getMessage(), null);
        return new ResponseEntity<>(Map.of("error", error), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
