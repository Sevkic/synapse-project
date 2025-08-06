# Synapse - AI-Powered Company Brain

Synapse is an AI-powered SaaS platform that acts as a "self-organizing brain" for companies. It connects to all of the company's knowledge and communication tools (Jira, Slack, Confluence, GitHub, etc.), ingests and understands the data, and allows employees to ask complex questions in natural language to get precise, source-backed answers.

## Architecture Overview

The project follows an Event Sourcing architecture with the following modules:

- **synapse-core**: Common domain models, events, and DTOs
- **ingestion-api**: High-performance API for ingesting events from all connectors  
- **connector-slack**: Connector that ingests messages from Slack channels
- **connector-jira**: Connector for Jira tickets and comments (planned)
- **connector-github**: Connector for GitHub commits and PRs (planned)
- **event-processor**: Background service for processing events and generating embeddings (planned)
- **query-api**: User-facing API for natural language queries (planned)

## Technology Stack

- **Backend**: Java 21, Spring Boot 3.3.2
- **Database**: PostgreSQL 16+ with pgvector extension
- **Event Sourcing**: Custom implementation with immutable event log
- **AI/ML**: RAG (Retrieval-Augmented Generation) architecture
- **Deployment**: Docker & Docker Compose

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- Slack Bot Token (for Slack connector)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd synapse-project
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your Slack bot token and channel ID
   ```

3. **Start PostgreSQL database**
   ```bash
   docker-compose up postgres -d
   ```

4. **Build the project**
   ```bash
   ./mvnw clean install
   ```

5. **Run the services locally**
   
   Start ingestion API:
   ```bash
   cd ingestion-api
   ../mvnw spring-boot:run
   ```
   
   Start Slack connector (in another terminal):
   ```bash
   cd connector-slack
   ../mvnw spring-boot:run
   ```

### Running with Docker

1. **Start all services**
   ```bash
   docker-compose --profile services up --build
   ```

2. **Start only the database**
   ```bash
   docker-compose up postgres -d
   ```

## Project Structure

```
synapse-project/
├── synapse-core/                 # Core domain models and DTOs
├── ingestion-api/               # Event ingestion service
├── connector-slack/             # Slack message connector
├── connector-jira/              # Jira connector (planned)
├── connector-github/            # GitHub connector (planned) 
├── event-processor/             # Event processing & embeddings (planned)
├── query-api/                   # Natural language query API (planned)
├── database/
│   └── schema/                  # Database schema files
├── docker-compose.yml           # Local development setup
└── README.md
```

## Database Schema

The system uses PostgreSQL with pgvector extension for vector similarity search:

- **events**: Immutable event log from all source systems
- **document_chunks**: Text chunks with vector embeddings for RAG
- **event_processing_state**: Tracks processing status of events
- **connector_sync_state**: Prevents duplicate ingestion from connectors

## API Endpoints

### Ingestion API (Port 8081)
- `POST /api/v1/ingest` - Ingest a SynapseEvent
- `GET /health` - Health check

### Slack Connector (Port 8082)
- Scheduled job runs every 1 minute to fetch new messages
- `GET /actuator/health` - Health check

## Configuration

### Slack Connector Setup

1. Create a Slack app at https://api.slack.com/apps
2. Add Bot Token Scopes: `channels:history`, `channels:read`
3. Install the app to your workspace
4. Copy the Bot User OAuth Token to your `.env` file
5. Get your channel ID and add it to `.env`

### Environment Variables

- `SLACK_BOT_TOKEN`: Your Slack bot token
- `SLACK_CHANNEL_ID`: The channel ID to monitor
- `DATABASE_URL`: PostgreSQL connection string
- `DATABASE_USERNAME`: Database username
- `DATABASE_PASSWORD`: Database password
- `INGESTION_API_URL`: URL of the ingestion API service

## Development

### Building Individual Modules

```bash
# Build core module
./mvnw clean install -pl synapse-core

# Build and run ingestion API
./mvnw clean package -pl ingestion-api -am
java -jar ingestion-api/target/ingestion-api-*.jar

# Build and run Slack connector
./mvnw clean package -pl connector-slack -am
java -jar connector-slack/target/connector-slack-*.jar
```

### Running Tests

```bash
./mvnw test
```

## Monitoring

All services include Spring Boot Actuator endpoints for monitoring:
- `/actuator/health` - Health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

## Next Steps

This MVP implements the foundational Event Sourcing architecture and Slack connector. Next development phases will include:

1. **Event Processor Service**: Process events and generate vector embeddings
2. **Query API Service**: Natural language query interface with RAG
3. **Additional Connectors**: Jira, GitHub, Confluence
4. **Production Deployment**: Kubernetes manifests and CI/CD pipeline
5. **UI/Frontend**: Web interface for querying and administration

## Contributing

1. Follow the existing code structure and conventions
2. Add tests for new functionality
3. Update documentation as needed
4. Ensure all services build and run successfully

## License

[Your License Here]