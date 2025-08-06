-- Enable the pgvector extension for vector operations
CREATE EXTENSION IF NOT EXISTS vector;

-- The immutable log of all events from all systems
-- This is the core of our Event Sourcing architecture
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    correlation_id UUID,
    source_system VARCHAR(50) NOT NULL,
    source_entity_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_timestamp TIMESTAMPTZ NOT NULL,
    version INT NOT NULL DEFAULT 1,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for efficient querying
CREATE INDEX idx_events_source_system ON events(source_system);
CREATE INDEX idx_events_event_type ON events(event_type);
CREATE INDEX idx_events_event_timestamp ON events(event_timestamp);
CREATE INDEX idx_events_correlation_id ON events(correlation_id) WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_events_created_at ON events(created_at);
CREATE INDEX idx_events_payload_gin ON events USING GIN(payload);

-- A table to hold chunks of text ready for vector search
-- This is where we store processed content with embeddings for RAG
CREATE TABLE document_chunks (
    id BIGSERIAL PRIMARY KEY,
    source_event_id UUID NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    chunk_text TEXT NOT NULL,
    chunk_metadata JSONB,
    embedding vector(384), -- Dimension for all-MiniLM-L6-v2 model (384 dimensions)
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- High-performance HNSW index for vector similarity search
CREATE INDEX idx_document_chunks_embedding ON document_chunks USING HNSW (embedding vector_l2_ops);
CREATE INDEX idx_document_chunks_source_event ON document_chunks(source_event_id);
CREATE INDEX idx_document_chunks_created_at ON document_chunks(created_at);

-- Table for tracking processing state of events
CREATE TABLE event_processing_state (
    event_id UUID PRIMARY KEY REFERENCES events(event_id) ON DELETE CASCADE,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED
    chunks_created INT NOT NULL DEFAULT 0,
    processed_at TIMESTAMPZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_event_processing_status ON event_processing_state(processing_status);
CREATE INDEX idx_event_processing_created_at ON event_processing_state(created_at);

-- Table for tracking connector sync state (to avoid duplicates)
CREATE TABLE connector_sync_state (
    id BIGSERIAL PRIMARY KEY,
    source_system VARCHAR(50) NOT NULL,
    sync_key VARCHAR(255) NOT NULL, -- e.g., channel_id for Slack, project_id for Jira
    last_sync_timestamp TIMESTAMPTZ NOT NULL,
    last_processed_id VARCHAR(255), -- Last processed message/ticket/commit ID
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(source_system, sync_key)
);

CREATE INDEX idx_connector_sync_source_system ON connector_sync_state(source_system);
CREATE INDEX idx_connector_sync_updated_at ON connector_sync_state(updated_at);