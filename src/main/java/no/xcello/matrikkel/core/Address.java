package no.xcello.matrikkel.core;

public record Address(
        String postadresse,
        String postnummer,
        String navn,
        String kommunenummer,
        int veinummer,
        int husnummer,
        String bokstav
) {
}
