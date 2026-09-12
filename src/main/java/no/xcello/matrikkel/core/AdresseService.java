package no.xcello.matrikkel.core;

import java.util.List;

public interface AdresseService {

    List<Adresse> search(String query, int treffPerSide);

    List<Seksjon> getSeksjoner(Adresse address);
}
