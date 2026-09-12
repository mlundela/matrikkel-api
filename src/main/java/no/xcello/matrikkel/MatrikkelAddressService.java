package no.xcello.matrikkel;

import no.xcello.matrikkel.core.Adresse;
import no.xcello.matrikkel.core.AdresseService;
import no.xcello.matrikkel.core.Seksjon;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatrikkelAddressService implements AdresseService {

    private final MatrikkelSearchClient searchClient;
    private final MatrikkelWebServiceClient webServiceClient;

    MatrikkelAddressService(MatrikkelSearchClient client, MatrikkelWebServiceClient webServiceClient) {
        this.searchClient = client;
        this.webServiceClient = webServiceClient;
    }

    @Override
    public List<Adresse> search(String query, int treffPerSide) {
        return searchClient.search(query, treffPerSide);
    }

    @Override
    public List<Seksjon> getSeksjoner(Adresse address) {
        return webServiceClient.hentSeksjoner(address);
    }
}
