package com.synapse.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QueryRequest(
        @NotBlank(message = "Question cannot be blank")
        @Size(min = 5, max = 1000, message = "Question must be between 5 and 1000 characters")
        @JsonProperty("question") 
        String question,
        
        @NotBlank(message = "User ID cannot be blank")
        @JsonProperty("userId") 
        String userId
) {
    @JsonCreator
    public QueryRequest(
            @JsonProperty("question") String question,
            @JsonProperty("userId") String userId
    ) {
        this.question = question;
        this.userId = userId;
    }
}