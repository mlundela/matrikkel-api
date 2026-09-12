package no.xcello.matrikkel.core;

public record Fraction(long teller, long nevner) {

    public static final Fraction ALT = new Fraction(1, 1);

    @Override
    public String toString() {
        return teller + "/" + nevner;
    }
}
