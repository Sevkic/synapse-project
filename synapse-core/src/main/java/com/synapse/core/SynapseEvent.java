package com.synapse.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record SynapseEvent(
        @NotNull 
        @JsonProperty("eventId") 
        UUID eventId,
        
        @JsonProperty("correlationId") 
        UUID correlationId,
        
        @NotNull 
        @JsonProperty("timestamp") 
        Instant timestamp,
        
        @NotBlank 
        @JsonProperty("sourceSystem") 
        String sourceSystem,
        
        @NotBlank 
        @JsonProperty("sourceEntityId") 
        String sourceEntityId,
        
        @NotBlank 
        @JsonProperty("eventType") 
        String eventType,
        
        @NotNull 
        @JsonProperty("version") 
        Integer version,
        
        @NotNull 
        @JsonProperty("payload") 
        JsonNode payload
) {
    @JsonCreator
    public SynapseEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("correlationId") UUID correlationId,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("sourceSystem") String sourceSystem,
            @JsonProperty("sourceEntityId") String sourceEntityId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("version") Integer version,
            @JsonProperty("payload") JsonNode payload
    ) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.correlationId = correlationId;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.sourceSystem = sourceSystem;
        this.sourceEntityId = sourceEntityId;
        this.eventType = eventType;
        this.version = version != null ? version : 1;
        this.payload = payload;
    }
    
    public static SynapseEventBuilder builder() {
        return new SynapseEventBuilder();
    }
    
    public static class SynapseEventBuilder {
        private UUID eventId;
        private UUID correlationId;
        private Instant timestamp;
        private String sourceSystem;
        private String sourceEntityId;
        private String eventType;
        private Integer version;
        private JsonNode payload;
        
        public SynapseEventBuilder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }
        
        public SynapseEventBuilder correlationId(UUID correlationId) {
            this.correlationId = correlationId;
            return this;
        }
        
        public SynapseEventBuilder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public SynapseEventBuilder sourceSystem(String sourceSystem) {
            this.sourceSystem = sourceSystem;
            return this;
        }
        
        public SynapseEventBuilder sourceEntityId(String sourceEntityId) {
            this.sourceEntityId = sourceEntityId;
            return this;
        }
        
        public SynapseEventBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public SynapseEventBuilder version(Integer version) {
            this.version = version;
            return this;
        }
        
        public SynapseEventBuilder payload(JsonNode payload) {
            this.payload = payload;
            return this;
        }
        
        public SynapseEvent build() {
            return new SynapseEvent(eventId, correlationId, timestamp, sourceSystem, 
                                  sourceEntityId, eventType, version, payload);
        }
    }
}