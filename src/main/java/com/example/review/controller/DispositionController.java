package com.example.review.controller;

import com.example.review.dto.DisposeRequest;
import com.example.review.service.DispositionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class DispositionController {

    private final DispositionService dispositionService;

    public DispositionController(DispositionService dispositionService) {
        this.dispositionService = dispositionService;
    }

    @PostMapping("/{id}/dispose")
    public ResponseEntity<Object> dispose(@PathVariable Long id,
                                          @RequestParam Long operatorId,
                                          @Valid @RequestBody DisposeRequest req) {
        try {
            return ResponseEntity.ok(dispositionService.dispose(id, operatorId, req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}