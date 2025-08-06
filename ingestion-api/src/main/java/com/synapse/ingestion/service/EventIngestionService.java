package com.synapse.ingestion.service;

import com.synapse.core.SynapseEvent;
import com.synapse.ingestion.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventIngestionService.class);
    
    private final EventRepository eventRepository;
    
    public EventIngestionService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }
    
    public void ingestEvent(SynapseEvent event) {
        logger.debug("Ingesting event: {} from {} with entity ID: {}", 
                    event.eventType(), event.sourceSystem(), event.sourceEntityId());
        
        try {
            eventRepository.save(event);
            logger.info("Successfully persisted event: {} with ID: {}", 
                       event.eventType(), event.eventId());
        } catch (Exception e) {
            logger.error("Failed to persist event: {} with ID: {}", 
                        event.eventType(), event.eventId(), e);
            throw new RuntimeException("Failed to persist event", e);
        }
    }
}