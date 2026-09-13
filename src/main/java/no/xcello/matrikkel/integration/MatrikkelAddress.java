package no.xcello.matrikkel.integration;

import com.fasterxml.jackson.annotation.JsonAlias;

public record MatrikkelAddress(
        @JsonAlias("poststed") String postalName,
        @JsonAlias("postnummer") String postalNumber,
        @JsonAlias("adressetekst") String name,
        @JsonAlias("kommunenummer") String municipalityNumber,
        @JsonAlias("adressekode") int code,
        @JsonAlias("nummer") int number,
        @JsonAlias("bokstav") String letter
) {
}
