package no.xcello.matrikkel.core;

import java.util.Collection;
import java.util.stream.Collectors;

public record Seksjon(
        int nummer,
        Brøk brøk,
        Collection<Eier> eiere,
        String bruksenhetNummer
) {
    public String ownerNames() {
        return eiere()
                .stream()
                .map(o -> o.person().navn())
                .collect(Collectors.joining(" & "));
    }
}
