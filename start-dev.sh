#!/bin/bash

echo "🐘 Starting PostgreSQL for development..."

# Start only PostgreSQL
docker-compose up postgres -d

echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 5

echo "✅ PostgreSQL is running!"
echo ""
echo "🛠️  Now you can build and run services locally:"
echo "  ./mvnw clean install"
echo "  cd ingestion-api && ../mvnw spring-boot:run"
echo "  # In another terminal:"
echo "  cd connector-slack && ../mvnw spring-boot:run"
echo ""
echo "📋 Database connection:"
echo "  URL: jdbc:postgresql://localhost:5432/synapse"
echo "  Username: synapse"
echo "  Password: synapse"