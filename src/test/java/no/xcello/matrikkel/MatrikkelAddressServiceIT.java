package no.xcello.matrikkel;

import no.xcello.matrikkel.core.Adresse;
import no.xcello.matrikkel.core.Seksjon;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        MatrikkelConfig.class,
        MatrikkelSearchClient.class,
        MatrikkelWebServiceClient.class,
        MatrikkelAddressService.class,
})
class MatrikkelAddressServiceIT {

    @Autowired MatrikkelAddressService service;

    @Test
    void name() {
        final List<Adresse> searchResult = service.search("Dokkeveien 1A", 5);
        assertThat(searchResult).isNotEmpty();
        final List<Seksjon> units = service.getSeksjoner(searchResult.getFirst());
        assertThat(units).isNotEmpty();
    }
}
