package no.xcello.matrikkel.core;

import java.util.List;

public interface Service {

    List<Address> search(String query, int size);

    List<Section> listSections(Address address);
}
