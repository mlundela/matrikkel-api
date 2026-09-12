package no.xcello.matrikkel.api;

import no.xcello.matrikkel.core.Adresse;
import no.xcello.matrikkel.core.AdresseService;
import no.xcello.matrikkel.core.Seksjon;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/adresse")
@RestController
public class AdresseRestController {

    private final AdresseService service;

    public AdresseRestController(AdresseService service) {
        this.service = service;
    }

    @GetMapping
    List<Adresse> søk(@RequestParam(name = "query") String query,
                      @RequestParam(name = "size", defaultValue = "5") int treffPerSide) {
        return service.search(query, treffPerSide);
    }

    @GetMapping("/{knr}/{veinr}/{husnr}/{bokstav}")
    List<Seksjon> hentSeksjoner(
            @PathVariable("knr") String knr,
            @PathVariable("veinr") Integer veinr,
            @PathVariable("husnr") Integer husnr,
            @PathVariable("bokstav") String bokstav
    ) {
        return service.getSeksjoner(new Adresse(null, null, null, knr, veinr, husnr, bokstav));
    }

    @GetMapping("/{knr}/{veinr}/{husnr}/")
    List<Seksjon> hentSeksjoner(
            @PathVariable("knr") String knr,
            @PathVariable("veinr") Integer veinr,
            @PathVariable("husnr") Integer husnr
    ) {
        return service.getSeksjoner(new Adresse(null, null, null, knr, veinr, husnr, ""));
    }
}
