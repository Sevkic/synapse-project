package com.synapse.ingestion.controller;

import com.synapse.core.SynapseEvent;
import com.synapse.ingestion.service.EventIngestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IngestionController {
    
    private static final Logger logger = LoggerFactory.getLogger(IngestionController.class);
    
    private final EventIngestionService eventIngestionService;
    
    public IngestionController(EventIngestionService eventIngestionService) {
        this.eventIngestionService = eventIngestionService;
    }
    
    @PostMapping("/ingest")
    public ResponseEntity<String> ingestEvent(@Valid @RequestBody SynapseEvent event) {
        try {
            logger.debug("Received event for ingestion: {} from {}", 
                        event.eventType(), event.sourceSystem());
            
            eventIngestionService.ingestEvent(event);
            
            logger.info("Successfully ingested event: {} with ID: {}", 
                       event.eventType(), event.eventId());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Event ingested successfully");
            
        } catch (Exception e) {
            logger.error("Failed to ingest event: {} with ID: {}", 
                        event.eventType(), event.eventId(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to ingest event: " + e.getMessage());
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Ingestion API is healthy");
    }
}