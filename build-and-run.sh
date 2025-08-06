#!/bin/bash

echo "🚀 Building and starting Synapse project..."

# Stop any running containers
echo "🛑 Stopping existing containers..."
docker-compose down

# Build and start everything
echo "🔨 Building and starting all services..."
docker-compose --profile services up --build

echo "✅ All services should be running now!"
echo ""
echo "📋 Available services:"
echo "  - PostgreSQL: localhost:5432"
echo "  - Ingestion API: http://localhost:8081"
echo "  - Slack Connector: http://localhost:8082"
echo ""
echo "🧪 Test endpoints:"
echo "  - Health: curl http://localhost:8081/health"
echo "  - Generate test data: curl -X POST http://localhost:8081/api/v1/test/generate-all-data"
echo "  - Generate Slack data: curl -X POST 'http://localhost:8081/api/v1/test/generate-slack-data?count=20'"