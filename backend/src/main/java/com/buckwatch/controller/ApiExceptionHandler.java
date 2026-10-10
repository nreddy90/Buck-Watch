package com.buckwatch.controller;
import com.buckwatch.service.ScenarioException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ScenarioException.class) public ResponseEntity<Map<String,Object>> scenario(ScenarioException e) { return ResponseEntity.badRequest().body(Map.of("error",Map.of("code",e.code(),"message",e.getMessage(),"choices",e.choices()))); }
 @ExceptionHandler(Exception.class) public ResponseEntity<Map<String,Object>> invalid(Exception e) { return ResponseEntity.badRequest().body(Map.of("error",Map.of("code","INVALID_REQUEST","message","Request was invalid. Check city, date, amounts, currency, and horizon.","choices",List.of()))); }
}
