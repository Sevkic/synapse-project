package com.synapse.github;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GitHubConnectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(GitHubConnectorApplication.class, args);
    }
}