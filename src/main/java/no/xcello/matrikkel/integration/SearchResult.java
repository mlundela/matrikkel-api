package no.xcello.matrikkel.integration;

import java.util.List;

public record SearchResult(
        Metadata metadata,
        List<MatrikkelAddress> adresser
) {
}
