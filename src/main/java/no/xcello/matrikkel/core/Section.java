package no.xcello.matrikkel.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collection;
import java.util.stream.Collectors;

public record Section(
        @JsonProperty("nummer")
        int nummer,
        @JsonProperty("brøk")
        Fraction brøk,
        @JsonProperty("eiere")
        Collection<Owner> eiere,
        @JsonProperty("bruksenhetNummer")
        String bruksenhetNummer
) {
    public String ownerNames() {
        return eiere()
                .stream()
                .map(o -> o.person().name())
                .collect(Collectors.joining(" & "));
    }
}
