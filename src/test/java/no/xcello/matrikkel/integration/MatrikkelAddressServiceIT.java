package no.xcello.matrikkel.integration;

import no.xcello.matrikkel.core.Fraction;
import no.xcello.matrikkel.core.Section;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Live test against Matrikkel prodtest. Owners are deliberately not validated, since ownership
 * changes over time. Seksjoner, sameiebrøk and bruksenhetsnummer are stable.
 */
@SpringBootTest(classes = {
        MatrikkelConfig.class,
        MatrikkelClient.class,
        MatrikkelServiceImpl.class,
})
class MatrikkelAddressServiceIT {

    private static final int NEVNER = 426;

    @Autowired MatrikkelServiceImpl service;

    @Test
    @DisplayName("Lists all seksjoner for 4601/10900/1/A with brøk and bruksenhetsnummer")
    void test1() {
        final List<Section> units = service.listSections("4601/10900/1/A");

        assertThat(units)
                .extracting(Section::nummer, Section::fraction, Section::bruksenhetNummer)
                .containsExactly(
                        tuple(1, fraction(42), "U0101"),
                        tuple(2, fraction(39), "H0101"),
                        tuple(3, fraction(53), "H0102"),
                        tuple(4, fraction(43), "H0201"),
                        tuple(5, fraction(56), "H0202"),
                        tuple(6, fraction(43), "H0301"),
                        tuple(7, fraction(56), "H0302"),
                        tuple(8, fraction(37), "L0101"),
                        tuple(9, fraction(57), "L0102"));
        assertThat(units.stream().mapToLong(s -> s.fraction().teller()).sum())
                .as("sameiebrøker sum to the whole")
                .isEqualTo(NEVNER);
    }

    private static Fraction fraction(long teller) {
        return new Fraction(teller, NEVNER);
    }
}
