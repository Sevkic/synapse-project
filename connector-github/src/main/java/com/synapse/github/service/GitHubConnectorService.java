package com.synapse.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synapse.core.SynapseEvent;
import com.synapse.core.constants.EventType;
import com.synapse.core.constants.SourceSystem;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class GitHubConnectorService {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubConnectorService.class);
    
    @Value("${github.token}")
    private String githubToken;
    
    @Value("${github.username}")
    private String githubUsername;
    
    @Value("${synapse.ingestion-api.url}")
    private String ingestionApiUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GitHubSyncStateService syncStateService;
    
    private GitHub github;
    private Map<String, Date> repositoryLastSync = new HashMap<>();
    
    public GitHubConnectorService(RestTemplate restTemplate, ObjectMapper objectMapper, 
                                 GitHubSyncStateService syncStateService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.syncStateService = syncStateService;
    }
    
    @Scheduled(fixedDelay = 300000) // Run every 5 minutes
    public void syncGitHubRepositories() {
        logger.debug("Starting GitHub repositories sync for user: {}", githubUsername);
        
        try {
            initializeGitHub();
            
            GHUser user = github.getUser(githubUsername);
            Map<String, GHRepository> repositories = user.getRepositories();
            
            logger.info("Found {} repositories for user: {}", repositories.size(), githubUsername);
            
            for (GHRepository repo : repositories.values()) {
                try {
                    syncRepository(repo);
                } catch (Exception e) {
                    logger.error("Failed to sync repository: {}", repo.getName(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("Failed to sync GitHub repositories", e);
        }
    }
    
    private void initializeGitHub() throws IOException {
        if (github == null) {
            github = new GitHubBuilder().withOAuthToken(githubToken).build();
            logger.info("GitHub API client initialized for user: {}", githubUsername);
        }
    }
    
    private void syncRepository(GHRepository repo) throws IOException {
        logger.debug("Syncing repository: {}", repo.getFullName());
        
        String repoKey = repo.getFullName();
        Date lastSync = repositoryLastSync.get(repoKey);
        
        // For first sync or if last sync was more than 1 hour ago, sync last 24 hours
        if (lastSync == null) {
            lastSync = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // 24 hours ago
        }
        
        // Sync commits
        syncCommits(repo, lastSync);
        
        // Sync pull requests
        syncPullRequests(repo, lastSync);
        
        // Update last sync timestamp
        repositoryLastSync.put(repoKey, new Date());
        
        logger.debug("Completed sync for repository: {}", repo.getFullName());
    }
    
    private void syncCommits(GHRepository repo, Date since) throws IOException {
        logger.debug("Syncing commits for repository: {} since {}", repo.getFullName(), since);
        
        PagedIterable<GHCommit> commits = repo.queryCommits()
            .since(since)
            .list();
        
        int commitCount = 0;
        for (GHCommit commit : commits) {
            try {
                SynapseEvent event = createCommitEvent(repo, commit);
                sendEventToIngestionApi(event);
                commitCount++;
            } catch (Exception e) {
                logger.error("Failed to process commit: {}", commit.getSHA1(), e);
            }
        }
        
        logger.info("Processed {} commits for repository: {}", commitCount, repo.getFullName());
    }
    
    private void syncPullRequests(GHRepository repo, Date since) throws IOException {
        logger.debug("Syncing pull requests for repository: {} since {}", repo.getFullName(), since);
        
        List<GHPullRequest> pullRequests = repo.getPullRequests(GHIssueState.ALL);
        int prCount = 0;
        
        for (GHPullRequest pr : pullRequests) {
            try {
                // Only sync PRs created or updated since our last sync
                if (pr.getCreatedAt().after(since) || pr.getUpdatedAt().after(since)) {
                    SynapseEvent event = createPullRequestEvent(repo, pr);
                    sendEventToIngestionApi(event);
                    prCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to process pull request: {}", pr.getNumber(), e);
            }
        }
        
        logger.info("Processed {} pull requests for repository: {}", prCount, repo.getFullName());
    }
    
    private SynapseEvent createCommitEvent(GHRepository repo, GHCommit commit) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        
        // Basic commit info
        payload.put("repository", repo.getFullName());
        payload.put("commitId", commit.getSHA1());
        payload.put("message", commit.getCommitShortInfo().getMessage());
        payload.put("url", commit.getHtmlUrl().toString());
        payload.put("branch", "main"); // Default, could be enhanced to detect actual branch
        
        // Author info
        GHCommit.ShortInfo shortInfo = commit.getCommitShortInfo();
        if (shortInfo.getAuthor() != null) {
            payload.put("author", shortInfo.getAuthor().getName());
            payload.put("authorEmail", shortInfo.getAuthor().getEmail());
        } else {
            payload.put("author", "unknown");
            payload.put("authorEmail", "unknown");
        }
        
        // File statistics
        if (commit.getFiles() != null && !commit.getFiles().isEmpty()) {
            payload.put("filesChanged", commit.getFiles().size());
            
            int additions = 0, deletions = 0;
            for (GHCommit.File file : commit.getFiles()) {
                additions += file.getLinesAdded();
                deletions += file.getLinesDeleted();
            }
            payload.put("additions", additions);
            payload.put("deletions", deletions);
        }
        
        // Repository metadata
        payload.put("repositoryDescription", repo.getDescription());
        payload.put("repositoryLanguage", repo.getLanguage());
        payload.put("repositoryStars", repo.getStargazersCount());
        payload.put("repositoryForks", repo.getForksCount());
        
        Instant commitTime = shortInfo.getCommitDate().toInstant();
        
        return SynapseEvent.builder()
                .sourceSystem(SourceSystem.GITHUB)
                .sourceEntityId(commit.getSHA1())
                .eventType(EventType.GITHUB_COMMIT_PUSHED)
                .timestamp(commitTime)
                .payload(payload)
                .build();
    }
    
    private SynapseEvent createPullRequestEvent(GHRepository repo, GHPullRequest pr) throws IOException {
        ObjectNode payload = objectMapper.createObjectNode();
        
        // Basic PR info
        payload.put("repository", repo.getFullName());
        payload.put("pullRequestNumber", pr.getNumber());
        payload.put("title", pr.getTitle());
        payload.put("body", pr.getBody() != null ? pr.getBody() : "");
        payload.put("state", pr.getState().toString());
        payload.put("url", pr.getHtmlUrl().toString());
        
        // Author info
        if (pr.getUser() != null) {
            payload.put("author", pr.getUser().getLogin());
        } else {
            payload.put("author", "unknown");
        }
        
        // Branch info
        payload.put("headBranch", pr.getHead().getRef());
        payload.put("baseBranch", pr.getBase().getRef());
        
        // Statistics
        payload.put("additions", pr.getAdditions());
        payload.put("deletions", pr.getDeletions());
        payload.put("changedFiles", pr.getChangedFiles());
        payload.put("commits", pr.getCommits());
        
        // Review info
        payload.put("reviewComments", pr.getReviewComments());
        payload.put("comments", pr.getCommentsCount());
        
        // Repository metadata
        payload.put("repositoryDescription", repo.getDescription());
        payload.put("repositoryLanguage", repo.getLanguage());
        payload.put("repositoryStars", repo.getStargazersCount());
        
        // Use created date for the event timestamp
        Instant eventTime = pr.getCreatedAt().toInstant();
        
        return SynapseEvent.builder()
                .sourceSystem(SourceSystem.GITHUB)
                .sourceEntityId("PR_" + repo.getName() + "_" + pr.getNumber())
                .eventType(EventType.GITHUB_PULL_REQUEST_OPENED)
                .timestamp(eventTime)
                .payload(payload)
                .build();
    }
    
    private void sendEventToIngestionApi(SynapseEvent event) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<SynapseEvent> request = new HttpEntity<>(event, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(
                ingestionApiUrl + "/api/v1/ingest", 
                request, 
                String.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Ingestion API returned non-success status: " + 
                                         response.getStatusCode());
            }
            
            logger.debug("Successfully sent event to ingestion API: {}", event.eventId());
            
        } catch (Exception e) {
            logger.error("Failed to send event to ingestion API: {}", event.eventId(), e);
            throw e;
        }
    }
    
    // Manual sync endpoint for testing
    public String manualSync() {
        try {
            syncGitHubRepositories();
            return "Manual sync completed successfully";
        } catch (Exception e) {
            logger.error("Manual sync failed", e);
            return "Manual sync failed: " + e.getMessage();
        }
    }
    
    // Repository analysis endpoint
    public Map<String, Object> analyzeRepository(String repositoryName) {
        try {
            initializeGitHub();
            
            GHRepository repo = github.getRepository(githubUsername + "/" + repositoryName);
            Map<String, Object> analysis = new HashMap<>();
            
            // Basic stats
            analysis.put("name", repo.getName());
            analysis.put("fullName", repo.getFullName());
            analysis.put("description", repo.getDescription());
            analysis.put("language", repo.getLanguage());
            analysis.put("stars", repo.getStargazersCount());
            analysis.put("forks", repo.getForksCount());
            analysis.put("openIssues", repo.getOpenIssueCount());
            analysis.put("size", repo.getSize());
            analysis.put("createdAt", repo.getCreatedAt());
            analysis.put("updatedAt", repo.getUpdatedAt());
            analysis.put("pushedAt", repo.getPushedAt());
            
            // Recent activity analysis
            Map<String, Object> activity = analyzeRecentActivity(repo);
            analysis.put("recentActivity", activity);
            
            // Code quality indicators
            Map<String, Object> codeQuality = analyzeCodeQuality(repo);
            analysis.put("codeQuality", codeQuality);
            
            return analysis;
            
        } catch (Exception e) {
            logger.error("Failed to analyze repository: {}", repositoryName, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to analyze repository: " + e.getMessage());
            return error;
        }
    }
    
    private Map<String, Object> analyzeRecentActivity(GHRepository repo) throws IOException {
        Map<String, Object> activity = new HashMap<>();
        
        // Last 30 days activity
        Date thirtyDaysAgo = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
        
        PagedIterable<GHCommit> recentCommits = repo.queryCommits()
            .since(thirtyDaysAgo)
            .list();
        
        int commitCount = 0;
        Set<String> contributors = new HashSet<>();
        
        for (GHCommit commit : recentCommits) {
            commitCount++;
            if (commit.getCommitShortInfo().getAuthor() != null) {
                contributors.add(commit.getCommitShortInfo().getAuthor().getName());
            }
        }
        
        activity.put("commitsLast30Days", commitCount);
        activity.put("activeContributorsLast30Days", contributors.size());
        
        // Pull requests activity
        try {
            List<GHPullRequest> recentPRs = repo.getPullRequests(GHIssueState.ALL);
            long recentPRCount = recentPRs.stream()
                .filter(pr -> {
                    try {
                        return pr.getCreatedAt().after(thirtyDaysAgo);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
            
            activity.put("pullRequestsLast30Days", recentPRCount);
        } catch (Exception e) {
            activity.put("pullRequestsLast30Days", 0);
        }
        
        return activity;
    }
    
    private Map<String, Object> analyzeCodeQuality(GHRepository repo) {
        Map<String, Object> quality = new HashMap<>();
        
        // Check for important files
        boolean hasReadme = false;
        boolean hasLicense = false;
        String licenseName = null;
        
        try {
            hasReadme = repo.getReadme() != null;
        } catch (Exception e) {
            // Ignore
        }
        
        try {
            hasLicense = repo.getLicense() != null;
            if (hasLicense) {
                licenseName = repo.getLicense().getName();
            }
        } catch (Exception e) {
            // Ignore
        }
        
        quality.put("hasReadme", hasReadme);
        quality.put("hasLicense", hasLicense);
        quality.put("licenseName", licenseName);
        
        // Check for CI/CD files (common patterns)
        try {
            boolean hasGitHubActions = repo.getDirectoryContent(".github/workflows") != null;
            quality.put("hasGitHubActions", hasGitHubActions);
        } catch (Exception e) {
            quality.put("hasGitHubActions", false);
        }
        
        // Repository health score (simple calculation)
        int healthScore = 0;
        if (hasReadme) healthScore += 25;
        if (hasLicense) healthScore += 25;
        if ((Boolean) quality.get("hasGitHubActions")) healthScore += 25;
        if (repo.getStargazersCount() > 0) healthScore += 25;
        
        quality.put("healthScore", healthScore);
        quality.put("healthGrade", getHealthGrade(healthScore));
        
        return quality;
    }
    
    private String getHealthGrade(int score) {
        if (score >= 90) return "A";
        else if (score >= 80) return "B";
        else if (score >= 70) return "C";
        else if (score >= 60) return "D";
        else return "F";
    }
}