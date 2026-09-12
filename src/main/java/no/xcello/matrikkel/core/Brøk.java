package no.xcello.matrikkel.core;

public record Brøk(long teller, long nevner) {

    public static final Brøk ALT = new Brøk(1, 1);

    @Override
    public String toString() {
        return teller + "/" + nevner;
    }
}
