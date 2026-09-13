package no.xcello.matrikkel.integration;

import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelBubbleId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelBubbleObject;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelBubbleObjectList;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelContext;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.adresse.AdresseId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.adresse.AdresseIdent;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.adresse.AdresseInfoTransfer;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.adresse.VegadresseIdent;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.bygning.Bruksenhet;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.bygning.koder.EtasjeplanKodeId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.Eierforhold;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.EierforholdList;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.Kontaktinstans;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.MatrikkelenhetId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.Matrikkelnummer;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.PersonIkkeTinglystEierforhold;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.PersonTinglystEierforhold;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.Seksjon;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.koder.EierforholdKodeId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.person.PersonId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.util.Andel;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.adresse.AdresseService;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.adresse.ServiceException;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.store.StoreService;
import no.xcello.matrikkel.core.Fraction;
import no.xcello.matrikkel.core.Owner;
import no.xcello.matrikkel.core.Person;
import no.xcello.matrikkel.core.Section;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.xml.datatype.DatatypeFactory;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MatrikkelClientTest {

    private static final long HJEMMELSHAVER = 1;
    private static final long FESTER = 2;

    @Mock StoreService storeService;
    @Mock AdresseService adresseService;
    @Mock MatrikkelContext context;

    MatrikkelClient client;

    @BeforeEach
    void setUp() {
        client = new MatrikkelClient(storeService, adresseService, context);
    }

    @Test
    @DisplayName("With bokstav, the id is sent as a VegadresseIdent with the given context")
    void test1() throws Exception {
        final AdresseId adresseId = stubAdresse();

        client.listSections("4601/10900/1/A");

        final ArgumentCaptor<AdresseIdent> ident = ArgumentCaptor.forClass(AdresseIdent.class);
        verify(adresseService).findAdresseIdForIdent(ident.capture(), same(context));
        verify(adresseService).findObjekterForAdresse(same(adresseId), same(context));
        assertThat(ident.getValue()).isInstanceOfSatisfying(VegadresseIdent.class, v -> {
            assertThat(v.getKommuneIdent().getKommunenummer()).isEqualTo("4601");
            assertThat(v.getAdressekode()).isEqualTo(10900);
            assertThat(v.getNummer()).isEqualTo(1);
            assertThat(v.getBokstav()).isEqualTo("A");
        });
    }

    @Test
    @DisplayName("Without bokstav, the VegadresseIdent has a null bokstav")
    void test2() throws Exception {
        stubAdresse();

        client.listSections("0301/10900/12");

        final ArgumentCaptor<AdresseIdent> ident = ArgumentCaptor.forClass(AdresseIdent.class);
        verify(adresseService).findAdresseIdForIdent(ident.capture(), same(context));
        assertThat(ident.getValue()).isInstanceOfSatisfying(VegadresseIdent.class, v -> {
            assertThat(v.getKommuneIdent().getKommunenummer()).isEqualTo("0301");
            assertThat(v.getNummer()).isEqualTo(12);
            assertThat(v.getBokstav()).isNull();
        });
    }

    @Test
    @DisplayName("A seksjon is joined with its bruksenhet and owner person")
    void test3() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(3, 10),
                        personTinglyst(HJEMMELSHAVER, andel(1, 2), dato("2020-01-15"), 900)),
                bruksenhet(100, 1, 1, 1));
        stubPerson(900, "Ola Nordmann", "01017012345");

        final List<Section> sections = client.listSections("4601/10900/1/A");

        assertThat(sections).containsExactly(new Section(
                1,
                new Fraction(3, 10),
                List.of(new Owner(
                        LocalDate.of(2020, 1, 15),
                        new Fraction(1, 2),
                        new Person("Ola Nordmann", "01017012345"))),
                "H0101"));
    }

    @Test
    @DisplayName("Sections are sorted by seksjonsnummer")
    void test4() throws Exception {
        stubAdresse(
                seksjon(300, 3, andel(1, 3), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                seksjon(100, 1, andel(1, 3), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                seksjon(200, 2, andel(1, 3), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                bruksenhet(300, 1, 3, 1),
                bruksenhet(100, 1, 1, 1),
                bruksenhet(200, 1, 2, 1));
        stubPerson(900, "Ola Nordmann", "1");

        final List<Section> sections = client.listSections("4601/10900/1/A");

        assertThat(sections).extracting(Section::nummer).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Bruksenhetsnummer maps etasjeplan K/L/U and zero-pads etasje and løpenummer")
    void test5() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(1, 3), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                seksjon(200, 2, andel(1, 3), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                seksjon(300, 3, andel(1, 3), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                bruksenhet(100, 2, 1, 3),
                bruksenhet(200, 3, 12, 34),
                bruksenhet(300, 4, 1, 1));
        stubPerson(900, "Ola Nordmann", "1");

        final List<Section> sections = client.listSections("4601/10900/1/A");

        assertThat(sections).extracting(Section::bruksenhetNummer).containsExactly("K0103", "L1234", "U0101");
    }

    @Test
    @DisplayName("Only owners with the highest eierforhold code are kept, and only they are looked up")
    void test6() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(1, 1),
                        personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900),
                        personTinglyst(FESTER, andel(1, 2), null, 901),
                        personTinglyst(FESTER, andel(1, 2), null, 902)),
                bruksenhet(100, 1, 1, 1));
        stubPerson(901, "Kari Fester", "901");
        stubPerson(902, "Per Fester", "902");

        final List<Section> sections = client.listSections("4601/10900/1/A");

        assertThat(sections.getFirst().owners())
                .extracting(o -> o.person().name())
                .containsExactlyInAnyOrder("Kari Fester", "Per Fester");
        verify(storeService, never()).getObject(argThat(id -> id != null && id.getValue() == 900), any());
    }

    @Test
    @DisplayName("An ikke-tinglyst owner uses its own andel and datoFra")
    void test7() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(1, 1),
                        personIkkeTinglyst(HJEMMELSHAVER, andel(2, 5), dato("2019-06-30"), 900)),
                bruksenhet(100, 1, 1, 1));
        stubPerson(900, "Ola Nordmann", "1");

        final Owner owner = client.listSections("4601/10900/1/A").getFirst().owners().iterator().next();

        assertThat(owner.fraction()).isEqualTo(new Fraction(2, 5));
        assertThat(owner.date()).isEqualTo(LocalDate.of(2019, 6, 30));
    }

    @Test
    @DisplayName("A kontaktinstans owns the whole seksjon")
    void test8() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(1, 1), kontaktinstans(HJEMMELSHAVER, dato("2018-03-01"), 900)),
                bruksenhet(100, 1, 1, 1));
        stubPerson(900, "Borettslaget", "987654321");

        final Owner owner = client.listSections("4601/10900/1/A").getFirst().owners().iterator().next();

        assertThat(owner).isEqualTo(new Owner(
                LocalDate.of(2018, 3, 1),
                Fraction.ALT,
                new Person("Borettslaget", "987654321")));
    }

    @Test
    @DisplayName("A missing datoFra gives a null owner date")
    void test9() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(1, 1), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                bruksenhet(100, 1, 1, 1));
        stubPerson(900, "Ola Nordmann", "1");

        final Owner owner = client.listSections("4601/10900/1/A").getFirst().owners().iterator().next();

        assertThat(owner.date()).isNull();
    }

    @Test
    @DisplayName("Other bubble objects and unmatched bruksenheter are ignored")
    void test10() throws Exception {
        final no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.person.Person unrelated =
                new no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.person.Person();
        unrelated.setId(id(new PersonId(), 555));
        stubAdresse(
                unrelated,
                bruksenhet(999, 1, 1, 1));

        assertThat(client.listSections("4601/10900/1/A")).isEmpty();
        verifyNoInteractions(storeService);
    }

    @Test
    @DisplayName("An unknown etasjeplan code fails with IllegalStateException")
    void test11() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(1, 1), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                bruksenhet(100, 5, 1, 1));
        stubPerson(900, "Ola Nordmann", "1");

        assertThatThrownBy(() -> client.listSections("4601/10900/1/A"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("5");
    }

    @Test
    @DisplayName("A seksjon without eierforhold fails with IllegalStateException")
    void test12() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(1, 1)),
                bruksenhet(100, 1, 1, 1));

        assertThatThrownBy(() -> client.listSections("4601/10900/1/A"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("An unknown Eierforhold subtype fails with IllegalStateException")
    void test13() throws Exception {
        final Eierforhold unknown = new Eierforhold();
        unknown.setEierforholdKodeId(id(new EierforholdKodeId(), HJEMMELSHAVER));
        stubAdresse(
                seksjon(100, 1, andel(1, 1), unknown),
                bruksenhet(100, 1, 1, 1));

        assertThatThrownBy(() -> client.listSections("4601/10900/1/A"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("A failing address id lookup is rethrown as RuntimeException")
    void test14() throws Exception {
        final ServiceException fault = new ServiceException("not found", null);
        given(adresseService.findAdresseIdForIdent(any(), same(context))).willThrow(fault);

        assertThatThrownBy(() -> client.listSections("4601/10900/1/A"))
                .isInstanceOf(RuntimeException.class)
                .hasCause(fault);
        verify(adresseService, never()).findObjekterForAdresse(any(), any());
    }

    @Test
    @DisplayName("A failing objekter-for-adresse lookup is rethrown as RuntimeException")
    void test15() throws Exception {
        final AdresseId adresseId = id(new AdresseId(), 42);
        final ServiceException fault = new ServiceException("boom", null);
        given(adresseService.findAdresseIdForIdent(any(), same(context))).willReturn(adresseId);
        given(adresseService.findObjekterForAdresse(adresseId, context)).willThrow(fault);

        assertThatThrownBy(() -> client.listSections("4601/10900/1/A"))
                .isInstanceOf(RuntimeException.class)
                .hasCause(fault);
    }

    @Test
    @DisplayName("A failing person lookup is rethrown as RuntimeException")
    void test16() throws Exception {
        stubAdresse(
                seksjon(100, 1, andel(1, 1), personTinglyst(HJEMMELSHAVER, andel(1, 1), null, 900)),
                bruksenhet(100, 1, 1, 1));
        given(storeService.getObject(any(), same(context)))
                .willThrow(new no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.store.ServiceException("boom", null));

        assertThatThrownBy(() -> client.listSections("4601/10900/1/A"))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.store.ServiceException.class);
    }

    private AdresseId stubAdresse(MatrikkelBubbleObject... bubbleObjects) throws Exception {
        final AdresseId adresseId = id(new AdresseId(), 42);
        final MatrikkelBubbleObjectList list = new MatrikkelBubbleObjectList();
        list.getItem().addAll(List.of(bubbleObjects));
        final AdresseInfoTransfer info = new AdresseInfoTransfer();
        info.setAdresseId(adresseId);
        info.setBubbleObjects(list);

        given(adresseService.findAdresseIdForIdent(any(), same(context))).willReturn(adresseId);
        given(adresseService.findObjekterForAdresse(adresseId, context)).willReturn(info);
        return adresseId;
    }

    private void stubPerson(long personId, String navn, String nummer) throws Exception {
        final no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.person.Person person =
                new no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.person.Person();
        person.setId(id(new PersonId(), personId));
        person.setNavn(navn);
        person.setNummer(nummer);
        given(storeService.getObject(argThat(id -> id != null && id.getValue() == personId), eq(context)))
                .willReturn(person);
    }

    private static Seksjon seksjon(long id, int seksjonsnummer, Andel sameiebrok, Eierforhold... eierforhold) {
        final Matrikkelnummer matrikkelnummer = new Matrikkelnummer();
        matrikkelnummer.setSeksjonsnummer(seksjonsnummer);
        final EierforholdList eiere = new EierforholdList();
        eiere.getItem().addAll(List.of(eierforhold));

        final Seksjon seksjon = new Seksjon();
        seksjon.setId(id(new MatrikkelenhetId(), id));
        seksjon.setMatrikkelnummer(matrikkelnummer);
        seksjon.setSameiebrok(sameiebrok);
        seksjon.setEierforhold(eiere);
        return seksjon;
    }

    private static Bruksenhet bruksenhet(long matrikkelenhetId, long etasjeplan, int etasjenummer, int lopenummer) {
        final Bruksenhet bruksenhet = new Bruksenhet();
        bruksenhet.setMatrikkelenhetId(id(new MatrikkelenhetId(), matrikkelenhetId));
        bruksenhet.setEtasjeplanKodeId(id(new EtasjeplanKodeId(), etasjeplan));
        bruksenhet.setEtasjenummer(etasjenummer);
        bruksenhet.setLopenummer(lopenummer);
        return bruksenhet;
    }

    private static PersonTinglystEierforhold personTinglyst(
            long kode, Andel andel, no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.LocalDate datoFra, long personId) {
        final PersonTinglystEierforhold e = new PersonTinglystEierforhold();
        e.setEierforholdKodeId(id(new EierforholdKodeId(), kode));
        e.setAndel(andel);
        e.setDatoFra(datoFra);
        e.setEierId(id(new PersonId(), personId));
        return e;
    }

    private static PersonIkkeTinglystEierforhold personIkkeTinglyst(
            long kode, Andel andel, no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.LocalDate datoFra, long personId) {
        final PersonIkkeTinglystEierforhold e = new PersonIkkeTinglystEierforhold();
        e.setEierforholdKodeId(id(new EierforholdKodeId(), kode));
        e.setAndel(andel);
        e.setDatoFra(datoFra);
        e.setEierId(id(new PersonId(), personId));
        return e;
    }

    private static Kontaktinstans kontaktinstans(
            long kode, no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.LocalDate datoFra, long personId) {
        final Kontaktinstans e = new Kontaktinstans();
        e.setEierforholdKodeId(id(new EierforholdKodeId(), kode));
        e.setDatoFra(datoFra);
        e.setEierId(id(new PersonId(), personId));
        return e;
    }

    private static Andel andel(long teller, long nevner) {
        final Andel andel = new Andel();
        andel.setTeller(teller);
        andel.setNevner(nevner);
        return andel;
    }

    private static no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.LocalDate dato(String iso) {
        final no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.LocalDate dato =
                new no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.LocalDate();
        dato.setDate(DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(iso));
        return dato;
    }

    private static <T extends MatrikkelBubbleId> T id(T id, long value) {
        id.setValue(value);
        return id;
    }
}
