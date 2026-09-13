package no.xcello.matrikkel.core;



import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record Owner(
        @JsonProperty("dato")
        LocalDate date,
        @JsonProperty("brøk")
        Fraction fraction,
        @JsonProperty("person")
        Person person
) {
}
