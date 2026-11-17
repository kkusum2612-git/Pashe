package com.pashe.app.identity;

import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String,String>> status(ResponseStatusException ex) {
        var response=ResponseEntity.status(ex.getStatusCode());
        if (ex.getStatusCode().value()==429) response.header("Retry-After","900");
        return response.body(Map.of("error","Request could not be completed"));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    ResponseEntity<Map<String,String>> invalid(Exception ex) { return ResponseEntity.badRequest().body(Map.of("error","Invalid request")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String,String>> conflict(Exception ex) { return ResponseEntity.status(409).body(Map.of("error","Account details conflict")); }
    @ExceptionHandler(MailException.class)
    ResponseEntity<Map<String,String>> mail(Exception ex) { return ResponseEntity.status(503).body(Map.of("error","Email service unavailable")); }
}
