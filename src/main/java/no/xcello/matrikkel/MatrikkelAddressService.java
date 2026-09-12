package no.xcello.matrikkel;

import no.xcello.matrikkel.core.Address;
import no.xcello.matrikkel.core.Service;
import no.xcello.matrikkel.core.Section;

import java.util.List;

@org.springframework.stereotype.Service
public class MatrikkelAddressService implements Service {

    private final MatrikkelSearchClient searchClient;
    private final MatrikkelWebServiceClient webServiceClient;

    MatrikkelAddressService(MatrikkelSearchClient client, MatrikkelWebServiceClient webServiceClient) {
        this.searchClient = client;
        this.webServiceClient = webServiceClient;
    }

    @Override
    public List<Address> search(String query, int maxHits) {
        return searchClient.search(query, maxHits);
    }

    @Override
    public List<Section> listSections(Address address) {
        return webServiceClient.listSectionsByAddress(address);
    }
}
