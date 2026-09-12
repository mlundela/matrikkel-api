package no.xcello.matrikkel.core;


import java.time.LocalDate;

public record Owner(
        LocalDate date,
        Fraction fraction,
        Person person
) {
}
