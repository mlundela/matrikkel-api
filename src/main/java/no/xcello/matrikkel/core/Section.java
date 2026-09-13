package no.xcello.matrikkel.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;

public record Section(
        @JsonProperty("nummer")
        int nummer,
        @JsonProperty("brøk")
        Fraction fraction,
        @JsonProperty("eiere")
        Collection<Owner> owners,
        @JsonProperty("bruksenhetNummer")
        String bruksenhetNummer
) {
}
