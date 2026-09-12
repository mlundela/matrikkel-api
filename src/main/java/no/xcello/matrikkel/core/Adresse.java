package no.xcello.matrikkel.core;

public record Adresse(
        String postadresse,
        String postnummer,
        String navn,
        String kommunenummer,
        int veinummer,
        int husnummer,
        String bokstav
) {
}
