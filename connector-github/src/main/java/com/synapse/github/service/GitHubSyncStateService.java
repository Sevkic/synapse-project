package com.synapse.github.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitHubSyncStateService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubSyncStateService.class);
    
    // For MVP, we'll use in-memory storage. In production, this should be persisted to database
    private final ConcurrentHashMap<String, Date> syncState = new ConcurrentHashMap<>();
    
    public Date getLastSyncTimestamp(String repositoryKey) {
        Date lastSync = syncState.get(repositoryKey);
        
        if (lastSync == null) {
            // First time running, start from 7 days ago to get recent activity
            Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            lastSync = Date.from(sevenDaysAgo);
            
            logger.info("No previous sync state found for repository: {}. Starting from: {}", 
                       repositoryKey, sevenDaysAgo);
        } else {
            logger.debug("Found previous sync timestamp for repository {}: {}", repositoryKey, lastSync);
        }
        
        return lastSync;
    }
    
    public void updateLastSyncTimestamp(String repositoryKey, Date timestamp) {
        syncState.put(repositoryKey, timestamp);
        logger.debug("Updated sync state for repository {}: {}", repositoryKey, timestamp);
    }
    
    public void clearSyncState(String repositoryKey) {
        syncState.remove(repositoryKey);
        logger.info("Cleared sync state for repository: {}", repositoryKey);
    }
    
    public void clearAllSyncState() {
        syncState.clear();
        logger.info("Cleared all sync state");
    }
}