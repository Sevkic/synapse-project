package com.synapse.slack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.model.Message;
import com.synapse.core.SynapseEvent;
import com.synapse.core.constants.EventType;
import com.synapse.core.constants.SourceSystem;
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
import java.util.List;
import java.util.UUID;

@Service
public class SlackConnectorService {
    
    private static final Logger logger = LoggerFactory.getLogger(SlackConnectorService.class);
    
    @Value("${slack.bot-token}")
    private String slackBotToken;
    
    @Value("${slack.channel-id}")
    private String channelId;
    
    @Value("${synapse.ingestion-api.url}")
    private String ingestionApiUrl;
    
    private final Slack slack;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final SlackSyncStateService syncStateService;
    
    private String lastMessageTimestamp;
    
    public SlackConnectorService(RestTemplate restTemplate, ObjectMapper objectMapper, 
                                SlackSyncStateService syncStateService) {
        this.slack = Slack.getInstance();
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.syncStateService = syncStateService;
    }
    
    @Scheduled(fixedDelay = 60000) // Run every 1 minute
    public void fetchSlackMessages() {
        logger.debug("Starting Slack message fetch for channel: {}", channelId);
        
        try {
            // Get the last sync timestamp to avoid duplicates
            String lastTimestamp = syncStateService.getLastSyncTimestamp(channelId);
            
            // Fetch conversation history
            var response = slack.methods(slackBotToken).conversationsHistory(req -> req
                    .channel(channelId)
                    .oldest(lastTimestamp)
                    .limit(100)
                    .inclusive(false) // Don't include the last message we already processed
            );
            
            if (!response.isOk()) {
                logger.error("Failed to fetch Slack messages: {}", response.getError());
                return;
            }
            
            List<Message> messages = response.getMessages();
            if (messages.isEmpty()) {
                logger.debug("No new messages found in channel: {}", channelId);
                return;
            }
            
            logger.info("Found {} new messages in channel: {}", messages.size(), channelId);
            
            String latestTimestamp = null;
            int processedCount = 0;
            
            // Process messages in reverse order (oldest first)
            for (int i = messages.size() - 1; i >= 0; i--) {
                Message message = messages.get(i);
                
                try {
                    SynapseEvent event = createSlackEvent(message);
                    sendEventToIngestionApi(event);
                    
                    latestTimestamp = message.getTs();
                    processedCount++;
                    
                } catch (Exception e) {
                    logger.error("Failed to process message: {}", message.getTs(), e);
                }
            }
            
            // Update sync state with the latest timestamp
            if (latestTimestamp != null) {
                syncStateService.updateLastSyncTimestamp(channelId, latestTimestamp);
                logger.info("Successfully processed {} messages. Latest timestamp: {}", 
                           processedCount, latestTimestamp);
            }
            
        } catch (SlackApiException e) {
            logger.error("Slack API error while fetching messages", e);
        } catch (IOException e) {
            logger.error("IO error while fetching messages", e);
        } catch (Exception e) {
            logger.error("Unexpected error while fetching messages", e);
        }
    }
    
    private SynapseEvent createSlackEvent(Message message) {
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("user", message.getUser() != null ? message.getUser() : "unknown");
            payload.put("text", message.getText() != null ? message.getText() : "");
            payload.put("timestamp", message.getTs());
            payload.put("channel", channelId);
            
            // Add thread timestamp if this is a thread reply
            if (message.getThreadTs() != null && !message.getThreadTs().equals(message.getTs())) {
                payload.put("threadTimestamp", message.getThreadTs());
                payload.put("isThreadReply", true);
            } else {
                payload.put("isThreadReply", false);
            }
            
            // Convert Slack timestamp to Instant
            Instant messageTimestamp = Instant.ofEpochSecond(
                (long) Double.parseDouble(message.getTs())
            );
            
            return SynapseEvent.builder()
                    .eventId(UUID.randomUUID())
                    .correlationId(UUID.randomUUID())
                    .timestamp(messageTimestamp)
                    .sourceSystem(SourceSystem.SLACK)
                    .sourceEntityId(message.getTs())
                    .eventType(EventType.SLACK_MESSAGE_POSTED)
                    .version(1)
                    .payload(payload)
                    .build();
                    
        } catch (Exception e) {
            logger.error("Failed to create SynapseEvent from Slack message: {}", message.getTs(), e);
            throw new RuntimeException("Failed to create SynapseEvent", e);
        }
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
}