package com.synapse.core.constants;

public final class EventType {
    public static final String SLACK_MESSAGE_POSTED = "SlackMessagePostedEvent";
    public static final String JIRA_TICKET_CREATED = "JiraTicketCreatedEvent";
    public static final String JIRA_TICKET_COMMENT_ADDED = "JiraTicketCommentAddedEvent";
    public static final String GITHUB_COMMIT_PUSHED = "GitHubCommitPushedEvent";
    public static final String GITHUB_PULL_REQUEST_OPENED = "GitHubPullRequestOpenedEvent";
    public static final String CONFLUENCE_PAGE_CREATED = "ConfluencePageCreatedEvent";
    public static final String CONFLUENCE_PAGE_UPDATED = "ConfluencePageUpdatedEvent";
    
    private EventType() {
        // Utility class
    }
}