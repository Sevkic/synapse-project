package com.synapse.ingestion.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.synapse.core.SynapseEvent;
import com.synapse.core.constants.EventType;
import com.synapse.core.constants.SourceSystem;
import com.synapse.ingestion.service.EventIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test")
public class TestDataController {
    
    private static final Logger logger = LoggerFactory.getLogger(TestDataController.class);
    
    private final EventIngestionService eventIngestionService;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    
    public TestDataController(EventIngestionService eventIngestionService, ObjectMapper objectMapper) {
        this.eventIngestionService = eventIngestionService;
        this.objectMapper = objectMapper;
    }
    
    @PostMapping("/generate-slack-data")
    public ResponseEntity<String> generateSlackData(@RequestParam(defaultValue = "10") int count) {
        try {
            List<String> users = List.of("nikola.sevic", "john.doe", "jane.smith", "mike.johnson", "sarah.wilson");
            List<String> channels = List.of("general", "development", "random", "synapse-project", "tech-talk");
            List<String> messages = List.of(
                "Hey team, how's the new feature coming along?",
                "I just pushed the latest changes to the repo. Please review when you get a chance.",
                "The database migration completed successfully in production.",
                "Can someone help me debug this issue with the API endpoints?",
                "Great job on the presentation today! 🎉",
                "I think we should consider using Redis for caching in the next iteration.",
                "The client meeting went well. They're happy with the progress.",
                "Don't forget about the sprint retrospective tomorrow at 2 PM.",
                "I found a potential security vulnerability in the auth service.",
                "The performance improvements are working great! Response times down by 40%.",
                "Quick question - what's our deployment schedule for this week?",
                "I've updated the documentation with the new API changes.",
                "The automated tests are passing on all environments.",
                "We might need to scale up the database before the weekend.",
                "Anyone available for a quick pair programming session?"
            );
            
            for (int i = 0; i < count; i++) {
                String user = users.get(random.nextInt(users.size()));
                String channel = channels.get(random.nextInt(channels.size()));
                String message = messages.get(random.nextInt(messages.size()));
                
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("user", user);
                payload.put("text", message);
                payload.put("channel", channel);
                payload.put("timestamp", String.valueOf(Instant.now().minusSeconds(random.nextInt(3600)).getEpochSecond()));
                payload.put("isThreadReply", random.nextBoolean());
                
                if (payload.get("isThreadReply").asBoolean()) {
                    payload.put("threadTimestamp", String.valueOf(Instant.now().minusSeconds(random.nextInt(7200)).getEpochSecond()));
                }
                
                SynapseEvent event = SynapseEvent.builder()
                        .sourceSystem(SourceSystem.SLACK)
                        .sourceEntityId("MSG_" + UUID.randomUUID().toString().substring(0, 8))
                        .eventType(EventType.SLACK_MESSAGE_POSTED)
                        .timestamp(Instant.now().minusSeconds(random.nextInt(3600)))
                        .payload(payload)
                        .build();
                
                eventIngestionService.ingestEvent(event);
            }
            
            logger.info("Generated {} dummy Slack events", count);
            return ResponseEntity.ok("Generated " + count + " dummy Slack events successfully");
            
        } catch (Exception e) {
            logger.error("Failed to generate dummy Slack data", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to generate dummy data: " + e.getMessage());
        }
    }
    
    @PostMapping("/generate-jira-data")
    public ResponseEntity<String> generateJiraData(@RequestParam(defaultValue = "5") int count) {
        try {
            List<String> authors = List.of("nikola.sevic@company.com", "john.doe@company.com", "jane.smith@company.com");
            List<String> projects = List.of("SYNAPSE", "AUTH", "API", "FRONTEND", "INFRA");
            List<String> ticketTypes = List.of("Bug", "Story", "Task", "Epic");
            List<String> statuses = List.of("Open", "In Progress", "Review", "Done");
            List<String> titles = List.of(
                "Fix authentication timeout issues",
                "Implement new user dashboard",
                "Optimize database queries for better performance",
                "Add unit tests for payment processing",
                "Update API documentation",
                "Investigate memory leak in background jobs",
                "Create responsive design for mobile users",
                "Implement rate limiting for API endpoints",
                "Add logging and monitoring to microservices",
                "Refactor legacy code in user management module"
            );
            List<String> descriptions = List.of(
                "Users are experiencing timeout issues when logging in during peak hours. Need to investigate and fix.",
                "Design and implement a new dashboard for users to view their account information and activity.",
                "The current database queries are taking too long. We need to optimize them for better performance.",
                "Add comprehensive unit tests to ensure payment processing works correctly in all scenarios.",
                "The API documentation is outdated and needs to be updated with the latest endpoint changes.",
                "There appears to be a memory leak in our background job processing that needs investigation.",
                "Our current design doesn't work well on mobile devices. Need to implement responsive design.",
                "Implement rate limiting to prevent API abuse and ensure fair usage across all clients.",
                "Add proper logging and monitoring to all microservices for better observability.",
                "The user management module has legacy code that needs to be refactored for maintainability."
            );
            
            for (int i = 0; i < count; i++) {
                String project = projects.get(random.nextInt(projects.size()));
                String ticketId = project + "-" + (100 + random.nextInt(900));
                String author = authors.get(random.nextInt(authors.size()));
                String title = titles.get(random.nextInt(titles.size()));
                String description = descriptions.get(random.nextInt(descriptions.size()));
                String status = statuses.get(random.nextInt(statuses.size()));
                String ticketType = ticketTypes.get(random.nextInt(ticketTypes.size()));
                
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("author", author);
                payload.put("title", title);
                payload.put("description", description);
                payload.put("status", status);
                payload.put("ticketType", ticketType);
                payload.put("project", project);
                payload.put("url", "https://company.atlassian.net/browse/" + ticketId);
                payload.put("priority", random.nextBoolean() ? "High" : "Medium");
                payload.put("assignee", authors.get(random.nextInt(authors.size())));
                
                SynapseEvent event = SynapseEvent.builder()
                        .sourceSystem(SourceSystem.JIRA)
                        .sourceEntityId(ticketId)
                        .eventType(EventType.JIRA_TICKET_CREATED)
                        .timestamp(Instant.now().minusSeconds(random.nextInt(86400 * 7))) // Last week
                        .payload(payload)
                        .build();
                
                eventIngestionService.ingestEvent(event);
                
                // Generate some comments for random tickets
                if (random.nextBoolean()) {
                    generateJiraComment(ticketId, author, event.timestamp());
                }
            }
            
            logger.info("Generated {} dummy Jira events", count);
            return ResponseEntity.ok("Generated " + count + " dummy Jira events successfully");
            
        } catch (Exception e) {
            logger.error("Failed to generate dummy Jira data", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to generate dummy data: " + e.getMessage());
        }
    }
    
    private void generateJiraComment(String ticketId, String originalAuthor, Instant ticketCreated) {
        List<String> commentAuthors = List.of("nikola.sevic@company.com", "john.doe@company.com", "jane.smith@company.com");
        List<String> comments = List.of(
            "I can take a look at this issue tomorrow.",
            "This is related to the work we did last sprint. Let me check the implementation.",
            "I've seen this issue before. The solution is to update the configuration in the database.",
            "Good catch! This needs to be prioritized for the next release.",
            "I'll assign this to my team. We have experience with similar issues.",
            "Can we schedule a quick call to discuss the requirements?",
            "The fix has been deployed to staging. Please test when you get a chance.",
            "I've created a pull request with the proposed solution.",
            "This issue is blocked by INFRA-456. We need to wait for that to be resolved first.",
            "I've updated the acceptance criteria based on our discussion."
        );
        
        String commentAuthor = commentAuthors.get(random.nextInt(commentAuthors.size()));
        String commentText = comments.get(random.nextInt(comments.size()));
        
        ObjectNode commentPayload = objectMapper.createObjectNode();
        commentPayload.put("author", commentAuthor);
        commentPayload.put("comment", commentText);
        commentPayload.put("ticketId", ticketId);
        commentPayload.put("url", "https://company.atlassian.net/browse/" + ticketId + "#comment-" + random.nextInt(1000));
        
        SynapseEvent commentEvent = SynapseEvent.builder()
                .sourceSystem(SourceSystem.JIRA)
                .sourceEntityId(ticketId + "_COMMENT_" + random.nextInt(1000))
                .eventType(EventType.JIRA_TICKET_COMMENT_ADDED)
                .timestamp(ticketCreated.plus(random.nextInt(48), ChronoUnit.HOURS))
                .payload(commentPayload)
                .build();
        
        eventIngestionService.ingestEvent(commentEvent);
    }
    
    @PostMapping("/generate-github-data")
    public ResponseEntity<String> generateGitHubData(@RequestParam(defaultValue = "8") int count) {
        try {
            List<String> authors = List.of("sevkic", "john-dev", "jane-smith", "mike-johnson");
            List<String> repositories = List.of("synapse-project", "auth-service", "api-gateway", "frontend-app");
            List<String> commitMessages = List.of(
                "Fix: Resolve authentication timeout issues in login flow",
                "Feature: Add new user dashboard with activity tracking",
                "Performance: Optimize database queries for 40% speed improvement",
                "Test: Add comprehensive unit tests for payment processing",
                "Docs: Update API documentation with latest endpoint changes",
                "Fix: Resolve memory leak in background job processing",
                "UI: Implement responsive design for mobile compatibility",
                "Security: Add rate limiting to prevent API abuse",
                "Monitoring: Add logging and observability to microservices",
                "Refactor: Clean up legacy code in user management module",
                "CI/CD: Update deployment pipeline configuration",
                "Dependencies: Upgrade Spring Boot to latest version",
                "Config: Update environment configurations for production",
                "Hotfix: Critical bug fix for payment processing",
                "Feature: Implement real-time notifications system"
            );
            
            for (int i = 0; i < count; i++) {
                String author = authors.get(random.nextInt(authors.size()));
                String repo = repositories.get(random.nextInt(repositories.size()));
                String message = commitMessages.get(random.nextInt(commitMessages.size()));
                String commitId = generateRandomHash();
                
                ObjectNode payload = objectMapper.createObjectNode();
                payload.put("author", author);
                payload.put("message", message);
                payload.put("repository", repo);
                payload.put("commitId", commitId);
                payload.put("url", "https://github.com/sevkic/" + repo + "/commit/" + commitId);
                payload.put("branch", random.nextBoolean() ? "main" : "develop");
                payload.put("filesChanged", random.nextInt(10) + 1);
                payload.put("additions", random.nextInt(200) + 1);
                payload.put("deletions", random.nextInt(50));
                
                SynapseEvent event = SynapseEvent.builder()
                        .sourceSystem(SourceSystem.GITHUB)
                        .sourceEntityId(commitId)
                        .eventType(EventType.GITHUB_COMMIT_PUSHED)
                        .timestamp(Instant.now().minusSeconds(random.nextInt(86400 * 3))) // Last 3 days
                        .payload(payload)
                        .build();
                
                eventIngestionService.ingestEvent(event);
            }
            
            logger.info("Generated {} dummy GitHub events", count);
            return ResponseEntity.ok("Generated " + count + " dummy GitHub events successfully");
            
        } catch (Exception e) {
            logger.error("Failed to generate dummy GitHub data", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to generate dummy data: " + e.getMessage());
        }
    }
    
    @PostMapping("/generate-all-data")
    public ResponseEntity<String> generateAllData() {
        try {
            generateSlackData(15);
            generateJiraData(8);
            generateGitHubData(12);
            
            return ResponseEntity.ok("Generated comprehensive test dataset successfully!\n" +
                    "- 15 Slack messages\n" +
                    "- 8 Jira tickets (with some comments)\n" +
                    "- 12 GitHub commits");
            
        } catch (Exception e) {
            logger.error("Failed to generate all dummy data", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to generate all dummy data: " + e.getMessage());
        }
    }
    
    private String generateRandomHash() {
        String chars = "0123456789abcdef";
        StringBuilder hash = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            hash.append(chars.charAt(random.nextInt(chars.length())));
        }
        return hash.toString();
    }
}