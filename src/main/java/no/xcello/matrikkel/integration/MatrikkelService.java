package no.xcello.matrikkel.integration;

import no.xcello.matrikkel.core.Address;
import no.xcello.matrikkel.core.Service;
import no.xcello.matrikkel.core.Section;

import java.util.List;

@org.springframework.stereotype.Service
public class MatrikkelService implements Service {

    private final MatrikkelSearchClient searchClient;
    private final MatrikkelWebServiceClient webServiceClient;

    MatrikkelService(MatrikkelSearchClient client, MatrikkelWebServiceClient webServiceClient) {
        this.searchClient = client;
        this.webServiceClient = webServiceClient;
    }

    @Override
    public List<Address> search(String query, int size) {
        return searchClient.search(query, size);
    }

    @Override
    public List<Section> listSections(Address address) {
        return webServiceClient.listSectionsByAddress(address);
    }
}
