package no.xcello.matrikkel.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Person(
        @JsonProperty("navn")
        String name,
        @JsonProperty("id")
        String id
) {
}
