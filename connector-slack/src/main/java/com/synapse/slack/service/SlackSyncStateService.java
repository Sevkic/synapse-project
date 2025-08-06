package com.synapse.slack.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SlackSyncStateService {
    
    private static final Logger logger = LoggerFactory.getLogger(SlackSyncStateService.class);
    
    // For MVP, we'll use in-memory storage. In production, this should be persisted to database
    private final ConcurrentHashMap<String, String> syncState = new ConcurrentHashMap<>();
    
    public String getLastSyncTimestamp(String channelId) {
        String lastTimestamp = syncState.get(channelId);
        
        if (lastTimestamp == null) {
            // First time running, start from 24 hours ago to avoid overwhelming initial load
            Instant oneDayAgo = Instant.now().minus(24, ChronoUnit.HOURS);
            lastTimestamp = String.valueOf(oneDayAgo.getEpochSecond());
            
            logger.info("No previous sync state found for channel: {}. Starting from: {}", 
                       channelId, oneDayAgo);
        } else {
            logger.debug("Found previous sync timestamp for channel {}: {}", channelId, lastTimestamp);
        }
        
        return lastTimestamp;
    }
    
    public void updateLastSyncTimestamp(String channelId, String timestamp) {
        syncState.put(channelId, timestamp);
        logger.debug("Updated sync state for channel {}: {}", channelId, timestamp);
    }
}