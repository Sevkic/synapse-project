package com.synapse.github.controller;

import com.synapse.github.service.GitHubConnectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/github")
public class GitHubAnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubAnalysisController.class);
    
    private final GitHubConnectorService gitHubConnectorService;
    
    public GitHubAnalysisController(GitHubConnectorService gitHubConnectorService) {
        this.gitHubConnectorService = gitHubConnectorService;
    }
    
    @PostMapping("/sync")
    public ResponseEntity<String> manualSync() {
        logger.info("Manual GitHub sync triggered");
        
        try {
            String result = gitHubConnectorService.manualSync();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Manual sync failed", e);
            return ResponseEntity.internalServerError()
                    .body("Manual sync failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/analyze/{repositoryName}")
    public ResponseEntity<Map<String, Object>> analyzeRepository(
            @PathVariable String repositoryName) {
        
        logger.info("Repository analysis requested for: {}", repositoryName);
        
        try {
            Map<String, Object> analysis = gitHubConnectorService.analyzeRepository(repositoryName);
            
            if (analysis.containsKey("error")) {
                return ResponseEntity.badRequest().body(analysis);
            }
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            logger.error("Repository analysis failed for: {}", repositoryName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Analysis failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("GitHub Connector is healthy");
    }
}