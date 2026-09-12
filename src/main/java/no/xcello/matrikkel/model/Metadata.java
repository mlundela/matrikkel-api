package no.xcello.matrikkel.model;

import com.fasterxml.jackson.annotation.JsonAlias;

public record Metadata(
        @JsonAlias("side") int page,
        @JsonAlias("treffPerSide") int pageSize,
        @JsonAlias("totaltAntallTreff") int hits
) {
}
