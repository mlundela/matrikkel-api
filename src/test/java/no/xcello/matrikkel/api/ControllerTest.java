package no.xcello.matrikkel.api;

import no.xcello.matrikkel.core.Fraction;
import no.xcello.matrikkel.core.MatrikkelService;
import no.xcello.matrikkel.core.Owner;
import no.xcello.matrikkel.core.Person;
import no.xcello.matrikkel.core.Section;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(Controller.class)
class ControllerTest {

    @Autowired MockMvcTester mvc;

    @MockitoBean MatrikkelService service;

    @Test
    @DisplayName("With bokstav, the full knr/veinr/husnr/bokstav id is passed to the service")
    void test1() {
        given(service.listSections("4601/10900/1/A")).willReturn(List.of());

        assertThat(mvc.get().uri("/api/adresse/4601/10900/1/A"))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("[]");

        verify(service).listSections("4601/10900/1/A");
    }

    @Test
    @DisplayName("Without bokstav, the id is knr/veinr/husnr with no trailing segment")
    void test2() {
        given(service.listSections("4601/10900/1")).willReturn(List.of());

        assertThat(mvc.get().uri("/api/adresse/4601/10900/1/"))
                .hasStatusOk();

        verify(service).listSections("4601/10900/1");
    }

    @Test
    @DisplayName("Sections are serialized with Norwegian JSON field names")
    void test3() {
        final Owner owner = new Owner(
                LocalDate.of(2020, 1, 15),
                new Fraction(1, 2),
                new Person("Ola Nordmann", "123"));
        final Section section = new Section(1, new Fraction(3, 10), List.of(owner), "H0101");
        given(service.listSections("4601/10900/1/A")).willReturn(List.of(section));

        assertThat(mvc.get().uri("/api/adresse/4601/10900/1/A"))
                .hasStatusOk()
                .bodyJson().isStrictlyEqualTo("""
                        [
                          {
                            "nummer": 1,
                            "brøk": { "teller": 3, "nevner": 10 },
                            "eiere": [
                              {
                                "dato": "2020-01-15",
                                "brøk": { "teller": 1, "nevner": 2 },
                                "person": { "navn": "Ola Nordmann", "id": "123" }
                              }
                            ],
                            "bruksenhetNummer": "H0101"
                          }
                        ]
                        """);
    }

    @Test
    @DisplayName("knr keeps leading zeros, veinr and husnr are parsed as integers")
    void test4() {
        given(service.listSections(anyString())).willReturn(List.of());

        assertThat(mvc.get().uri("/api/adresse/0301/010900/01/"))
                .hasStatusOk();

        verify(service).listSections("0301/10900/1");
    }

    @Test
    @DisplayName("Without bokstav, a missing trailing slash gives 404")
    void test5() {
        assertThat(mvc.get().uri("/api/adresse/4601/10900/1"))
                .hasStatus(HttpStatus.NOT_FOUND);

        verify(service, never()).listSections(anyString());
    }

    @Test
    @DisplayName("Non-numeric veinr gives 400")
    void test6() {
        assertThat(mvc.get().uri("/api/adresse/4601/abc/1/A"))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Non-numeric husnr gives 400")
    void test7() {
        assertThat(mvc.get().uri("/api/adresse/4601/10900/x/"))
                .hasStatus(HttpStatus.BAD_REQUEST);

        verifyNoInteractions(service);
    }
}
