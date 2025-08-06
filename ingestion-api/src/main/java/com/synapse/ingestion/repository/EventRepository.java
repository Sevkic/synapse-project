package com.synapse.ingestion.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.synapse.core.SynapseEvent;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;

@Repository
public class EventRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(EventRepository.class);
    
    private static final String INSERT_EVENT_SQL = """
        INSERT INTO events (
            event_id, correlation_id, source_system, source_entity_id, 
            event_type, event_timestamp, version, payload
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    public EventRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
    public void save(SynapseEvent event) {
        try {
            PGobject payloadJson = new PGobject();
            payloadJson.setType("jsonb");
            payloadJson.setValue(objectMapper.writeValueAsString(event.payload()));
            
            int rowsAffected = jdbcTemplate.update(
                INSERT_EVENT_SQL,
                event.eventId(),
                event.correlationId(),
                event.sourceSystem(),
                event.sourceEntityId(),
                event.eventType(),
                event.timestamp(),
                event.version(),
                payloadJson
            );
            
            if (rowsAffected != 1) {
                throw new DataAccessException("Expected 1 row to be affected, but " + rowsAffected + " were affected") {};
            }
            
            logger.debug("Successfully saved event: {} with ID: {}", 
                        event.eventType(), event.eventId());
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event payload for event ID: {}", event.eventId(), e);
            throw new RuntimeException("Failed to serialize event payload", e);
        } catch (SQLException e) {
            logger.error("Failed to set payload as JSONB for event ID: {}", event.eventId(), e);
            throw new RuntimeException("Failed to set payload as JSONB", e);
        } catch (DataAccessException e) {
            logger.error("Database error while saving event ID: {}", event.eventId(), e);
            throw e;
        }
    }
}