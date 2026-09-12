package no.xcello.matrikkel.core;


import java.time.LocalDate;

public record Eier(
        LocalDate dato,
        Brøk brøk,
        Person person
) {
}
