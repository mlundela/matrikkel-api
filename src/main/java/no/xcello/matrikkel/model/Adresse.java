package no.xcello.matrikkel.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Adresse(
        @JsonAlias("poststed") String postalName,
        @JsonAlias("postnummer") String postalNumber,
        @JsonAlias("adressetekst") String name,
        @JsonAlias("kommunenummer") String municipalityNumber,
        @JsonAlias("adressekode") int code,
        @JsonAlias("nummer") int number,
        @JsonAlias("bokstav") String letter
) {
}
