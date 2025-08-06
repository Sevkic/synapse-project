package com.synapse.core.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QueryResponse(
        @NotBlank
        @JsonProperty("answer") 
        String answer,
        
        @NotNull
        @Valid
        @JsonProperty("sources") 
        List<SourceDTO> sources
) {
    @JsonCreator
    public QueryResponse(
            @JsonProperty("answer") String answer,
            @JsonProperty("sources") List<SourceDTO> sources
    ) {
        this.answer = answer;
        this.sources = sources != null ? sources : List.of();
    }
}