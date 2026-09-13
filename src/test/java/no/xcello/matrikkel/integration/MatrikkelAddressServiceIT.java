package no.xcello.matrikkel.integration;

import no.xcello.matrikkel.core.Address;
import no.xcello.matrikkel.core.Section;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        MatrikkelConfig.class,
        MatrikkelSearchClient.class,
        MatrikkelWebServiceClient.class,
        MatrikkelService.class,
})
class MatrikkelAddressServiceIT {

    @Autowired MatrikkelService service;

    @Test
    void name() {
        final List<Address> searchResult = service.search("Dokkeveien 1A", 5);
        assertThat(searchResult).isNotEmpty();
        final List<Section> units = service.listSections(searchResult.getFirst());
        assertThat(units).isNotEmpty();
    }
}
