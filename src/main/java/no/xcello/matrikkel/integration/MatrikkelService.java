package no.xcello.matrikkel.integration;

import no.xcello.matrikkel.core.Section;
import no.xcello.matrikkel.core.Service;

import java.util.List;

@org.springframework.stereotype.Service
public class MatrikkelService implements Service {

    private final MatrikkelClient webServiceClient;

    MatrikkelService(MatrikkelClient webServiceClient) {
        this.webServiceClient = webServiceClient;
    }

    @Override
    public List<Section> listSections(String matrikkelId) {
        return webServiceClient.listSections(matrikkelId);
    }
}
