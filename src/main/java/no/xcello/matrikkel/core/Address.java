package no.xcello.matrikkel.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Address(
        @JsonProperty("postadresse")
        String postadresse,
        @JsonProperty("postnummer")
        String postnummer,
        @JsonProperty("navn")
        String navn,
        @JsonProperty("kommunenummer")
        String kommunenummer,
        @JsonProperty("veinummer")
        int veinummer,
        @JsonProperty("husnummer")
        int husnummer,
        @JsonProperty("bokstav")
        String bokstav
) {
}
