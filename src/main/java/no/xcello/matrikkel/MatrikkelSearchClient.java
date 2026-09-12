package no.xcello.matrikkel;

import no.xcello.matrikkel.core.Adresse;
import no.xcello.matrikkel.model.SearchResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Component
class MatrikkelSearchClient {

    private final RestClient client;

    MatrikkelSearchClient() {
        client = RestClient.builder().build();
    }

    List<Adresse> search(String query, int treffPerSide) {
        final URI uri = UriComponentsBuilder
                .fromUri(URI.create("https://ws.geonorge.no/adresser/v1/sok"))
                .queryParam("sok", query)
                .queryParam("treffPerSide", treffPerSide)
                .encode()
                .build()
                .toUri();

        final SearchResult searchResult = client.get()
                .uri(uri)
                .retrieve()
                .body(SearchResult.class);

        return searchResult
                .adresser()
                .stream()
                .map(this::transform)
                .toList();
    }

    Adresse transform(no.xcello.matrikkel.model.Adresse a) {
        return new Adresse(
                a.postalName(),
                a.postalNumber(),
                a.name(),
                a.municipalityNumber(),
                a.code(),
                a.number(),
                a.letter()
        );
    }
}
