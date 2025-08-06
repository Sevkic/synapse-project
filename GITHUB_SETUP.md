# GitHub Connector Setup

## 🔧 Quick Setup

### 1. Generate GitHub Personal Access Token

1. Go to GitHub Settings: https://github.com/settings/tokens
2. Click "Generate new token (classic)"
3. Give it a name like "Synapse Connector"
4. Select scopes:
   - `repo` (Full control of private repositories)
   - `read:org` (Read org and team membership)
   - `read:user` (Read user profile data)
5. Click "Generate token"
6. **IMPORTANT**: Copy the token immediately (you won't see it again!)

### 2. Configure Environment

Update your `.env` file:
```bash
# Replace with your actual token
GITHUB_TOKEN=ghp_your_token_here
GITHUB_USERNAME=sevkic
```

## 🚀 Testing the GitHub Connector

### Option 1: Local Development
```bash
# Start PostgreSQL and Ingestion API first
./start-dev.sh
cd ingestion-api && ../mvnw spring-boot:run &

# Start GitHub Connector
cd connector-github && ../mvnw spring-boot:run
```

### Option 2: Docker (All Services)
```bash
# Make sure to set GITHUB_TOKEN in .env first!
docker compose --profile services up --build
```

## 🧪 API Endpoints

### Manual Sync
Trigger immediate sync of all repositories:
```bash
curl -X POST http://localhost:8083/api/v1/github/sync
```

### Repository Analysis
Get AI-powered analysis of a specific repository:
```bash
curl http://localhost:8083/api/v1/github/analyze/synapse-project
```

### Health Check
```bash
curl http://localhost:8083/actuator/health
```

## 🔍 What the GitHub Connector Does

### Automated Sync (Every 5 minutes)
- ✅ Fetches all your GitHub repositories
- ✅ Syncs recent commits (last 24 hours initially, then incremental)
- ✅ Syncs pull requests (created or updated recently)
- ✅ Sends events to Synapse for AI analysis

### Repository Analysis Features
- 📊 **Activity Metrics**: Commits, contributors, PRs in last 30 days
- 🏥 **Health Score**: Based on README, license, CI/CD, stars
- 🔍 **Code Quality**: Detects important files and best practices
- ⭐ **Insights**: Stars, forks, language, size analysis

### Event Data Captured
Each GitHub event includes:
- Repository metadata (name, description, language, stars)
- Author information and timestamps
- Code statistics (additions, deletions, files changed)
- Comprehensive context for AI analysis

## 🤖 AI Analysis Capabilities

The GitHub connector enables AI to understand:
- **Development Patterns**: Who commits what, when, and how often
- **Code Quality Trends**: Improvement or degradation over time
- **Team Collaboration**: PR reviews, discussions, contributor patterns
- **Project Health**: Maintenance status, community engagement
- **Technical Decisions**: Commit messages reveal architectural choices

## ⚠️ Important Notes

1. **Token Security**: Never commit your GitHub token to version control
2. **Rate Limits**: GitHub has API rate limits (5000/hour for authenticated requests)
3. **Privacy**: Only repositories you have access to will be synced
4. **Storage**: All data is stored locally in your PostgreSQL database

## 🚨 Troubleshooting

### "Unauthorized" errors:
- Check your GitHub token is valid and not expired
- Ensure token has correct scopes (repo, read:org, read:user)

### "Repository not found":
- Verify repository name spelling
- Check you have access to the repository
- Ensure username is correct in configuration

### No events being created:
- Check ingestion-api is running on port 8081
- Verify PostgreSQL is accessible on port 5433
- Look at GitHub connector logs for error messages