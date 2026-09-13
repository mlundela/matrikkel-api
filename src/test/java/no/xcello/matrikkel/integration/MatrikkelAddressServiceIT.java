package no.xcello.matrikkel.integration;

import no.xcello.matrikkel.core.Section;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        MatrikkelConfig.class,
        MatrikkelClient.class,
        MatrikkelServiceImpl.class,
})
class MatrikkelAddressServiceIT {

    @Autowired MatrikkelServiceImpl service;

    @Test
    void name() {
        final List<Section> units = service.listSections("4601/10900/1/A");
        assertThat(units).isNotEmpty();
    }
}
