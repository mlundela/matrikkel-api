package no.xcello.matrikkel.model;

import java.util.List;

public record SearchResult(
        Metadata metadata,
        List<Adresse> adresser
) {
}
