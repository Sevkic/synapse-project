package com.synapse.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SourceDTO(
        @NotBlank
        @JsonProperty("sourceSystem") 
        String sourceSystem,
        
        @NotBlank
        @JsonProperty("sourceUrl") 
        String sourceUrl,
        
        @NotBlank
        @JsonProperty("snippet") 
        String snippet
) {
    @JsonCreator
    public SourceDTO(
            @JsonProperty("sourceSystem") String sourceSystem,
            @JsonProperty("sourceUrl") String sourceUrl,
            @JsonProperty("snippet") String snippet
    ) {
        this.sourceSystem = sourceSystem;
        this.sourceUrl = sourceUrl;
        this.snippet = snippet;
    }
}