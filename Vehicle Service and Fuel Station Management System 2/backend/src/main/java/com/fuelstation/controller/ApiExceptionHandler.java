package com.fuelstation.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {
    @ExceptionHandler(com.fuelstation.util.InputValidation.InvalidFields.class)
    public ResponseEntity<?> fields(com.fuelstation.util.InputValidation.InvalidFields error) { return ResponseEntity.badRequest().body(Map.of("success",false,"message",error.getMessage(),"fields",error.fields())); }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<?> photoTooLarge(Exception error) { return ResponseEntity.status(413).body(Map.of("success",false,"message","Use a PNG, JPEG or WebP photo under 5 MB")); }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, java.time.DateTimeException.class})
    public ResponseEntity<?> invalid(RuntimeException e) { return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage() == null ? "Invalid request" : e.getMessage())); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> forbidden(AccessDeniedException e) { return ResponseEntity.status(403).body(Map.of("success", false, "message", "You do not have permission for this record or action")); }
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<?> validation(jakarta.validation.ConstraintViolationException e) { return ResponseEntity.badRequest().body(Map.of("success",false,"message",e.getConstraintViolations().stream().map(v -> v.getMessage()).distinct().collect(java.util.stream.Collectors.joining("; ")))); }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> malformed(Exception e) { return ResponseEntity.badRequest().body(Map.of("success",false,"message","Enter valid values for the requested fields")); }
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<?> concurrent(Exception e) { return ResponseEntity.status(409).body(Map.of("success",false,"message","This record changed. Reload it and try again.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException e) { return ResponseEntity.status(409).body(Map.of("success", false, "message", "This record conflicts with an existing record or is still used by another operation")); }
}
