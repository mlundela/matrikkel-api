package no.xcello.matrikkel.integration;

import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.LocalDate;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelBubbleObject;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.MatrikkelContext;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.adresse.AdresseId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.adresse.AdresseInfoTransfer;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.adresse.VegadresseIdent;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.bygning.Bruksenhet;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.kommune.KommuneIdent;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.*;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.person.PersonId;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.util.Andel;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.adresse.AdresseService;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.adresse.ServiceException;
import no.statkart.matrikkel.matrikkelapi.wsapi.v1.service.store.StoreService;
import no.xcello.matrikkel.core.*;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
class MatrikkelClient implements MatrikkelService {

    private static final Logger log = LoggerFactory.getLogger(MatrikkelClient.class);

    private final StoreService storeService;
    private final AdresseService adresseService;
    private final MatrikkelContext context;

    MatrikkelClient(StoreService storeService, AdresseService adresseService, MatrikkelContext context) {
        this.storeService = storeService;
        this.adresseService = adresseService;
        this.context = context;
    }

    private static Fraction getFraction(Eierforhold e) {
        return switch (e) {
            case TinglystEierforhold t -> transformBrøk(t.getAndel());
            case IkkeTinglystEierforhold t -> transformBrøk(t.getAndel());
            case Kontaktinstans t -> Fraction.ALT;
            default -> throw new IllegalStateException("Unexpected value: " + e);
        };
    }

    private static java.time.LocalDate getDate(Eierforhold e) {
        return switch (e) {
            case TinglystEierforhold t -> transformDato(t.getDatoFra());
            case IkkeTinglystEierforhold t -> transformDato(t.getDatoFra());
            case Kontaktinstans t -> transformDato(t.getDatoFra());
            default -> throw new IllegalStateException("Unexpected value: " + e);
        };
    }

    private static java.time.LocalDate transformDato(LocalDate date) {
        return date == null ? null : java.time.LocalDate.of(
                date.getDate().getYear(),
                date.getDate().getMonth(),
                date.getDate().getDay()
        );
    }

    private static Fraction transformBrøk(Andel andel) {
        return new Fraction(andel.getTeller(), andel.getNevner());
    }

    @Override
    public List<Section> listSections(String matrikkel) {
        final AdresseId adresseId = getAdresseId(matrikkel);
        final AdresseInfoTransfer info = getAdresseInfoTransfer(adresseId);

        for (Object o : info.getBubbleObjects().getItem()) {
            if (o instanceof Bruksenhet bruksenhet) {
                System.out.println("B\t" + bruksenhet.getMatrikkelenhetId().getValue());
            }
        }

        for (Object o : info.getBubbleObjects().getItem()) {
            if (o instanceof no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.Seksjon s) {
                System.out.println("M\t" + s.getId().getValue());
            }
        }


        List<Section> seksjoner = filterSections(info);
        seksjoner.sort(Comparator.comparing(Section::nummer));
        log.info("Finn seksjoner for adresse: {}", matrikkel);
        for (Section s : seksjoner) {
            log.info("Fant seksjon: {}", s);
        }
        return seksjoner;
    }

    private List<Section> filterSections(AdresseInfoTransfer info) {
        List<Section> out = new ArrayList<>();

        Map<Long, Bruksenhet> bs = new HashMap<>();
        Map<Long, no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.Seksjon> ss = new HashMap<>();

        for (MatrikkelBubbleObject obj : info.getBubbleObjects().getItem()) {
            if (obj instanceof final no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.Seksjon s) {
                ss.put(s.getId().getValue(), s);
//                out.add(transformSeksjon(seksjon));
            }
            if (obj instanceof Bruksenhet b) {
                bs.put(b.getMatrikkelenhetId().getValue(), b);
            }
        }

        for (no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.matrikkelenhet.Seksjon s : ss.values()) {
            final long id = s.getId().getValue();
            final Bruksenhet b = bs.get(id);
            out.add(new Section(
                    s.getMatrikkelnummer().getSeksjonsnummer(),
                    transformBrøk(s.getSameiebrok()),
                    transformEierforhold(s.getEierforhold().getItem()),
                    getBruksenhetNummer(b)
            ));
        }
        return out;
    }

    private @NonNull String getBruksenhetNummer(Bruksenhet b) {
        return etasjeplan((int) b.getEtasjeplanKodeId().getValue()) + String.format("%02d", b.getEtasjenummer()) + String.format("%02d", b.getLopenummer());
    }

    private String etasjeplan(int kode) {
        return switch (kode) {
            case 1 -> "H";
            case 2 -> "K";
            case 3 -> "L";
            case 4 -> "U";
            default -> throw new IllegalStateException("Unexpected value: " + kode);
        };
    }


    /**
     * Viss eier kun har hjemmel til festerett, så filtrer vekk hjemmelshaver for eiendomsrett.
     * Vi antar at det er det er koden av høyest verdi som er reel eier.
     */
    private List<Owner> transformEierforhold(List<Eierforhold> liste) {

        final long max = liste.stream()
                .map(e -> e.getEierforholdKodeId().getValue())
                .max(Long::compareTo)
                .orElseThrow(() -> new IllegalStateException("Fant ingen eierforoholdkoder"));

        return liste
                .parallelStream()
                .filter(e -> e.getEierforholdKodeId().getValue() == max)
                .map(this::transformEierforhold)
                .toList();
    }

    private Owner transformEierforhold(Eierforhold e) {
        return new Owner(
                getDate(e),
                getFraction(e),
                getPerson(e)
        );
    }

    private Person getPerson(Eierforhold e) {
        return switch (e) {
            case PersonTinglystEierforhold t -> toPerson(t.getEierId());
            case PersonIkkeTinglystEierforhold t -> toPerson(t.getEierId());
            case Kontaktinstans t -> toPerson(t.getEierId());
            default -> throw new IllegalStateException("Unexpected value: " + e);
        };
    }

    private Person toPerson(PersonId pId) {
        try {
            final MatrikkelBubbleObject object = storeService.getObject(pId, context);
            final no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.person.Person p = (no.statkart.matrikkel.matrikkelapi.wsapi.v1.domain.person.Person) object;
            return new Person(p.getNavn(), p.getNummer());
        } catch (Exception e) {
            log.error("Failed to fetch person for id=" + pId.getValue(), e);
            throw new RuntimeException(e);
        }
    }

    private AdresseInfoTransfer getAdresseInfoTransfer(AdresseId adresseId) {
        try {
            return adresseService.findObjekterForAdresse(adresseId, context);
        } catch (ServiceException e) {
            log.error("Failed to list units by address ID: " + adresseId.getValue(), e);
            throw new RuntimeException(e);
        }
    }

    private AdresseId getAdresseId(String matrikkel) {
        // 4601/10900/1/A
        final String[] parts = matrikkel.split("/");
        try {
            KommuneIdent bergen = new KommuneIdent();
            bergen.setKommunenummer(parts[0]);

            final VegadresseIdent v = new VegadresseIdent();
            v.setKommuneIdent(bergen);
            v.setAdressekode(Integer.parseInt(parts[1]));
            v.setNummer(Integer.parseInt(parts[2]));
            v.setBokstav(parts.length == 3 ? null : parts[3]);

            return adresseService.findAdresseIdForIdent(v, context);

        } catch (ServiceException e) {
            log.error("Failed to find address ID for address: " + matrikkel, e);
            throw new RuntimeException(e);
        }
    }
}
