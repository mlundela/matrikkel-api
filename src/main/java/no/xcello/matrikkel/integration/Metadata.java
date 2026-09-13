package no.xcello.matrikkel.integration;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Metadata(
        @JsonAlias("side") int page,
        @JsonAlias("treffPerSide") int pageSize,
        @JsonAlias("totaltAntallTreff") int hits
) {
}
