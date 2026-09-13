package no.xcello.matrikkel.api;

import no.xcello.matrikkel.core.Section;
import no.xcello.matrikkel.core.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/adresse")
@RestController
public class Controller {

    private final Service service;

    public Controller(Service service) {
        this.service = service;
    }

    @GetMapping("/{knr}/{veinr}/{husnr}/{bokstav}")
    List<Section> listSections(
            @PathVariable("knr") String knr,
            @PathVariable("veinr") Integer veinr,
            @PathVariable("husnr") Integer husnr,
            @PathVariable("bokstav") String bokstav
    ) {
        return service.listSections(knr + "/" + veinr + "/" + husnr + "/" + bokstav);
    }

    @GetMapping("/{knr}/{veinr}/{husnr}/")
    List<Section> listSections(
            @PathVariable("knr") String knr,
            @PathVariable("veinr") Integer veinr,
            @PathVariable("husnr") Integer husnr
    ) {
        return service.listSections(knr + "/" + veinr + "/" + husnr);
    }
}
