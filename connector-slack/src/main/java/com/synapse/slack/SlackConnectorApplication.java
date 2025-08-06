package com.synapse.slack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SlackConnectorApplication {
    public static void main(String[] args) {
        SpringApplication.run(SlackConnectorApplication.class, args);
    }
}