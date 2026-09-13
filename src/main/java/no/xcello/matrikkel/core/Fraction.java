package no.xcello.matrikkel.core;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Fraction(

        @JsonProperty("teller")
        long teller,

        @JsonProperty("nevner")
        long nevner
) {

    public static final Fraction ALT = new Fraction(1, 1);

    @Override
    public String toString() {
        return teller + "/" + nevner;
    }
}
