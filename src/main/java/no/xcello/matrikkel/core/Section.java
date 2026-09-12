package no.xcello.matrikkel.core;

import java.util.Collection;
import java.util.stream.Collectors;

public record Section(
        int nummer,
        Fraction brøk,
        Collection<Owner> eiere,
        String bruksenhetNummer
) {
    public String ownerNames() {
        return eiere()
                .stream()
                .map(o -> o.person().name())
                .collect(Collectors.joining(" & "));
    }
}
