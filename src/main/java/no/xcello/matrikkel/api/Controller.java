package no.xcello.matrikkel.api;

import no.xcello.matrikkel.core.Address;
import no.xcello.matrikkel.core.Service;
import no.xcello.matrikkel.core.Section;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/adresse")
@RestController
public class Controller {

    private final Service service;

    public Controller(Service service) {
        this.service = service;
    }

    @GetMapping
    List<Address> søk(@RequestParam(name = "query") String query,
                      @RequestParam(name = "size", defaultValue = "5") int treffPerSide) {
        return service.search(query, treffPerSide);
    }

    @GetMapping("/{knr}/{veinr}/{husnr}/{bokstav}")
    List<Section> hentSeksjoner(
            @PathVariable("knr") String knr,
            @PathVariable("veinr") Integer veinr,
            @PathVariable("husnr") Integer husnr,
            @PathVariable("bokstav") String bokstav
    ) {
        return service.listSections(new Address(null, null, null, knr, veinr, husnr, bokstav));
    }

    @GetMapping("/{knr}/{veinr}/{husnr}/")
    List<Section> hentSeksjoner(
            @PathVariable("knr") String knr,
            @PathVariable("veinr") Integer veinr,
            @PathVariable("husnr") Integer husnr
    ) {
        return service.listSections(new Address(null, null, null, knr, veinr, husnr, ""));
    }
}
